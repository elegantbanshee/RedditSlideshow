var images = [];
var index = 0;
var firstRun = true;

function get_images() {
    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
          var json = JSON.parse(this.responseText);
          images = json;
        }
    }
    request.open("POST", "/api/data");
    request.setRequestHeader("Content-Type", "text/plain");
    var reg = /https?:\/\/[A-Za-z\.:\d]*\/r\/(.*)/
    request.send(reg.exec(window.location.href)[1]);
}

function play() {
    if (images.length > 0) {
        var img = document.getElementById("image");
        var video = document.getElementById("video");

        var isImage = images[index].search(".mp4") === -1;
        var isCurrentImage = img.style.display === "block";

        if (!isCurrentImage && !video.ended && video.currentTime > 0)
            return;

        if (isCurrentImage && !img.complete)
            return;

        if (isImage) {
            var newImg = document.createElement("img");
            newImg.id = "image"
            document.body.removeChild(img);
            document.body.appendChild(newImg);
            img = newImg;

            img.src = images[index];
            img.style.display = "block";
            video.style.display = "none";
        }
        else {
            video.src = images[index];
            video.style.display = "block";
            img.style.display = "none";
            video.play();
        }
        index++;
    }
}

function set_timeinterval() {
    setInterval(play, 5000);
}

function main() {
    get_images();
    set_timeinterval();
}

main();
