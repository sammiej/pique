
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.util.UUID;

import play.api.libs.iteratee.RunQueue;
import redis.clients.jedis.BinaryJedis;
import services.dataAccess.RedisAccessObject;
import services.dataAccess.proto.PostProto.Post;
import services.dataAccess.proto.PostListProto.PostList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



/**
 * Created by erik on 02/11/16.
 * <p>
 * Tests functionality of services.dataAccess.RedisAccessObject
 * on a real Redis instance.
 * <p>
 * In addition to the running Redis instance, the instance's
 * URL and port must be specified as environment variables:
 * <p>
 * redis_url=""
 * redis_port=""
 * <p>
 * data_source="redis"
 * must also be set.
 * <p>
 * Each test assumes boolean redisTestsIncluded is true; if false, no tests will run.
 */
public class RedisAccessTest {
    private static final Integer numTestPosts = 10;     // MUST be greater than 1.

    // generate unique ID within test namespace
    private static final String testKeyString = "test:" + UUID.randomUUID().toString();
    private static boolean redisTestsIncluded = false;

    // Sample test values
    private static final List<Post> posts = new ArrayList<>();
    private static PostList postList;

    // Class under test
    private static String redisUrl = System.getenv("redis_url");
    private static Integer redisPort = Integer.valueOf(System.getenv("redis_port"));
    private static RedisAccessObject redisAccessObject;

    // direct connection to Redis for maintenance
    private static BinaryJedis directToRedis;

    @BeforeClass
    public static void redisTestSetUp() {

        // Initialize object under test and direct connection
        redisTestsIncluded = System.getenv("data_source").equals("redis");
        redisAccessObject = new RedisAccessObject();
        directToRedis = new BinaryJedis(redisUrl, redisPort);

        // Initialize sample test values
        List<Post> tempPosts = new ArrayList<>();
        PostList.Builder postListBuilder = PostList.newBuilder();

        // build list of posts and postlists
        for (int i = 0; i < numTestPosts; i++) {
            Post.Builder postBuilder = Post.newBuilder();
            postBuilder.setId(String.valueOf(i));
            postBuilder.addHashtag("#id" + String.valueOf(i));
            postBuilder.addText("This is test post " + String.valueOf(i));

            tempPosts.add(postBuilder.build());
        }

        postListBuilder.addAllPosts(tempPosts);

        postList = postListBuilder.build();
        posts.addAll(tempPosts);

        assert (numTestPosts > 1); // we must have more than one test post
    }

    // Delete test key from data store before and after each test
    @Before
    @After
    public void emptyRedisTestKey() {
        assumeTrue(redisTestsIncluded);

        directToRedis.connect();
        directToRedis.del(testKeyString.getBytes());
        directToRedis.disconnect();
    }

    @Test
    public void addPostToEmptyRedis() {
        assumeTrue(redisTestsIncluded);

        // Add new post to completely empty store. Should create new key entry, with new post at front of key
        redisAccessObject.addNewPost(testKeyString, posts.get(0));
        assertEquals(posts.get(0), redisAccessObject.getAllPosts(testKeyString).get(0));
    }

    @Test
    public void addPostToNonEmptyRedis() {
        assumeTrue(redisTestsIncluded);

        // Initialize store with multiple posts, and add a single key
        redisAccessObject.addNewPosts(testKeyString, posts);
        redisAccessObject.addNewPost(testKeyString, posts.get(0));

        // new post has been added to end of posts
        assertEquals(posts.get(0), redisAccessObject.getAllPosts(testKeyString).get(numTestPosts));
    }

    @Test
    public void addPostsToEmptyRedis() {
        assumeTrue(redisTestsIncluded);

        // add multiple posts to an empty key. Should create new key entry, with posts in order
        redisAccessObject.addNewPosts(testKeyString, posts);

        assertEquals(posts, redisAccessObject.getAllPosts(testKeyString));
    }

    @Test
    public void addPostsToNonEmptyRedis() {
        assumeTrue(redisTestsIncluded);

        // Add 2 sets of posts
        redisAccessObject.addNewPosts(testKeyString, posts);
        redisAccessObject.addNewPosts(testKeyString, posts);

        // new posts have been added in order and succeed old posts
        assertEquals(posts, redisAccessObject.getAllPosts(testKeyString).subList(numTestPosts, 2 * numTestPosts));
    }

    @Test
    public void popEmptyPostRedis() {
        assumeTrue(redisTestsIncluded);

        // Need to see that we handle an empty response correctly
        assertFalse(redisAccessObject.popOldestPost(testKeyString).isPresent());
    }

    @Test
    public void popNonEmptyPostRedis() {
        assumeTrue(redisTestsIncluded);

        // add posts
        redisAccessObject.addNewPosts(testKeyString, posts);

        // see if we pop them in order
        for (int i = 0; i < numTestPosts; i++) {
            assertEquals(Optional.of(posts.get(i)), redisAccessObject.popOldestPost(testKeyString));
        }
    }

    @Test
    public void addPostListToEmptyRedis() {
        assumeTrue(redisTestsIncluded);

        // add posts to empty keyspace; should create new keyspace with those posts in order
        redisAccessObject.addNewPosts(testKeyString, posts);
        assertEquals(posts, redisAccessObject.getAllPosts(testKeyString));
    }

    @Test
    public void addPostListToNonEmptyRedis() {
        assumeTrue(redisTestsIncluded);

        // initialize temporary list with posts, andd add one post at index 0
        ArrayList<Post> testList = new ArrayList<>(posts);
        testList.add(0, posts.get(0));  // add first element to beginning of test list

        // add first post to redis, then remaining posts
        redisAccessObject.addNewPost(testKeyString, posts.get(0));
        redisAccessObject.addNewPosts(testKeyString, posts);

        // check if posts were added in correct order
        assertEquals(testList, redisAccessObject.getAllPosts(testKeyString));
    }

    @Test
    public void getEmptyListofPosts() {
        assumeTrue(redisTestsIncluded);

        // check for proper empty return value
        List<Post> emptyList = redisAccessObject.getAllPosts(testKeyString);
        assertEquals(new ArrayList<Post>(), emptyList);
    }

    @Test
    public void getNonEmptyListofPosts() {
        assumeTrue(redisTestsIncluded);

        // check for successful return, in-order
        redisAccessObject.addNewPosts(testKeyString, posts);
        assertEquals(posts, redisAccessObject.getAllPosts(testKeyString));
    }

    @Test
    public void popOldestPostFromEmpty() {
        assumeTrue(redisTestsIncluded);

        // check for proper error handling on empty keyspace
        assertEquals(Optional.empty(), redisAccessObject.popOldestPost(testKeyString));
    }

    @Test
    public void popOldestPostFromNonEmpty() {
        assumeTrue(redisTestsIncluded);

        // initialize with posts, and pop one post
        redisAccessObject.addNewPosts(testKeyString, posts);
        redisAccessObject.popOldestPost(testKeyString);

        // pop second post to see if popped in order
        assertEquals(Optional.of(posts.get(1)), redisAccessObject.popOldestPost(testKeyString));
    }

    @Test
    public void peekAtEmptyPostList() {
        assumeTrue(redisTestsIncluded);

        // check for error handling on empty PostList
        assertFalse(redisAccessObject.peekAtPostList(testKeyString).isPresent());
    }

    @Test
    public void peekAtNonEmptyPostList() {
        assumeTrue(redisTestsIncluded);

        // initialize postlist
        redisAccessObject.addNewPostList(testKeyString, postList);

        // get postlist; test twice to ensure we are not popping.
        assertEquals(Optional.of(postList), redisAccessObject.peekAtPostList(testKeyString));
        assertEquals(Optional.of(postList), redisAccessObject.peekAtPostList(testKeyString));
    }

}
