var favorite = {};

favorite.handleFavorite = function () {
    var storage = window.localStorage;
    var url = images[index]; // simple.js
    var faves = storage.getItem("favorites");
    if (faves == null)
        faves = "[]";
    faves = JSON.parse(faves);
    if (faves.indexOf(url) === -1)
        faves.push(url);
    else
        faves.splice(faves.indexOf(url), 1);
    storage.setItem("favorites", JSON.stringify(faves));
};