if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

function topFunction() {

    var all = document.getElementsByClassName("post_headline");

    for (var i=0, max=all.length; i < max; i++) {
         all[i].innerText = "Top Post";
    }

    //TopContentController controller = new TopContentController();
    //byte[] content = controller.content();
    //topContent.render("string");
}

function trendingFunction() {
    title.innerText = "Trending Content";
}

function hashtagFunction() {
    title.innerText = "Hashtag Content";
}
