package SortingTests;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assume.assumeTrue;
import services.dataAccess.AbstractDataAccess;
import services.dataAccess.InMemoryAccessObject;
import services.dataAccess.proto.PostProto.Post;
import services.sorting.PostSorter.AbstractPostSorter;
import services.sorting.PostSorter.TopPostSorter;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static services.dataAccess.TestDataGenerator.generateListOfPosts;
import static services.PublicConstants.TOP;

public class TopPostSorterTest {

    private AbstractPostSorter sorter;
    private AbstractDataAccess data;

    @Before
    public void topPostSorterTestSetup() {
        data = new InMemoryAccessObject();
        sorter = new TopPostSorter(data);
    }

    @Test
    public void testSortTopPostsZeroValues() {
        List<Post> posts = generateListOfPosts(10);
        List<Post> zeroedPosts = new ArrayList<>();

        // set popularity parameters to 0
        posts.forEach(post -> {
            post = post.toBuilder()
                    .setNumComments(0)
                    .setNumLikes(0)
                    .setNumShares(0)
                    .build();
            zeroedPosts.add(post);

        });

        // sort zeroed posts; result should be an empty list
        Map<String, List<Post>> sorted = sorter.sort(zeroedPosts);

        assertEquals(Collections.emptyList(), sorted.get(TOP));
    }

    @Test
    public void testSortTopPostsPositiveCase() {
        int prevScore = Integer.MAX_VALUE;
        List<Post> posts = generateListOfPosts(10);
        Map<String, List<Post>> sorted = sorter.sort(posts);

        // Posts should be sorted by decreasing popularity score
        for (Post post : sorted.get(TOP)) {
            int popularityScore = post.getPopularityScore();
            assertTrue(popularityScore <= prevScore);
            prevScore = popularityScore;
        }
    }

    @Test
    public void testLoadTopPosts() {
        int numPosts = 10;

        // ignore this test if numPosts is more than one page
        assumeTrue(numPosts <= AbstractPostSorter.getPageLimit());

        // generate posts
        List<Post> posts = generateListOfPosts(numPosts);
        Map<String, List<Post>> sorted = new HashMap<>();
        sorted.put(TOP, posts);

        sorter.load(sorted);

        // assert that what is stored matches what was added, in proper order
        List<Post> stored = data.getAllDisplayPostLists(TOP).get(0).getPostsList();
        assertEquals(posts, stored);

    }

    @Test
    public void testPopularityFilter() {
        List<Post> posts = generateListOfPosts(10);
        Map<String, List<Post>> sorted = sorter.sort(posts);
        int popularityThreshold = TopPostSorter.getPopularityThreshold();

        // All posts should be at or above the popularity threshold
        for (Post post : sorted.get(TOP)) {
            assertTrue(post.getPopularityScore() >= popularityThreshold);
        }

    }
}
