var images = [];
var index = 0;
var playInterval = null;
var page = "";
var downloading = false;

function clearIntervalAndPlay() {
    clearInterval(playInterval);
    playInterval = setInterval(play, 5000);
    play(true);
}

function handleButtonClick() {
    index = parseInt(this.innerText) - 1;
    clearIntervalAndPlay();
}

function addIndexButtons() {
    var buttonContainer = document.getElementById("buttons");
    buttonContainer.innerHTML = "";

    for (var buttonIndex = 0; buttonIndex < images.length; buttonIndex++) {
        var button = document.createElement("div");
        button.className = "button";
        button.innerHTML = buttonIndex + 1;
        button.addEventListener("click", handleButtonClick);
        buttonContainer.appendChild(button);
    }
}

function get_images() {
    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
          var json = JSON.parse(this.responseText);
          for (var imageIndex = 0; imageIndex < json.data.length; imageIndex++)
              images.push(json.data[imageIndex]);
          page = json.after;
          addIndexButtons();
          downloading = false;
        }
    }
    request.open("POST", "/api/data");
    request.setRequestHeader("Content-Type", "text/plain");
    var reg = /https?:\/\/[A-Za-z\.:\d]*\/r\/(.*)/;
    request.send(reg.exec(window.location.href)[1] + ";" + page);
    downloading = true;
}

function setTitle() {
    var title = document.getElementById("title");
    title.innerHTML = "/r/" + images[index].subreddit + " | " + images[index].title;
}

function play(force) {
    if (images.length > 0) {
        var img = document.getElementById("image");
        var video = document.getElementById("video");

        // CHeck if a stall is in order
        var isCurrentImage = img.style.display === "block";

        if (!isCurrentImage && !video.ended && video.currentTime > 0 && !force)
            return;

        if (isCurrentImage && !img.complete && !force)
            return;

        // Update to next image
        var isImage = images[index].url.search(".mp4") === -1;

        if (isImage) {
            var newImg = document.createElement("img");
            newImg.id = "image"
            document.body.removeChild(img);
            document.body.appendChild(newImg);
            img = newImg;

            img.src = images[index].url;
            img.style.display = "block";
            video.style.display = "none";
        }
        else {
            video.src = images[index].url;
            video.style.display = "block";
            img.style.display = "none";
            video.play();
        }

        setTitle();

        index++;
        if (images.length === index)
            get_images();
    }
}

function onVideoEnded() {
    clearIntervalAndPlay();
}

function initPlayLoop() {
    playInterval = setInterval(play, 5000);

    var video = document.getElementById("video");
    video.addEventListener("ended", onVideoEnded);
}

function main() {
    get_images();
    initPlayLoop();
}

main();
