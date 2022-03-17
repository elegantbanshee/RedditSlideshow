var favorite = {};

favorite.indexOf = function (faves, url) {
    for (var indexFav = 0; indexFav < faves.length; indexFav++)
        if (faves[indexFav].url === url)
            return indexFav;
    return -1;
};

favorite.handleFavorite = function () {
    var storage = window.localStorage;
    var url = images[index - 1]; // simple.js
    var faves = storage.getItem("favorites");
    if (faves == null)
        faves = "[]";
    faves = JSON.parse(faves);
    if (favorite.indexOf(faves, url.url) === -1)
        faves.push(url);
    else
        faves.splice(favorite.indexOf(faves, url.url), 1);
    storage.setItem("favorites", JSON.stringify(faves));
};