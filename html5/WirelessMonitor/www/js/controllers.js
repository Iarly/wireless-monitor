angular.module('starter.controllers', [])

.controller('DashCtrl', function ($scope, GPS) {

    var position = new google.maps.LatLng(-19.9377813, -43.981598);

    function initialize() {
        var mapOptions = {
            center: position,
            zoom: 19,
            mapTypeId: google.maps.MapTypeId.ROADMAP
        };
        var map = new google.maps.Map(document.getElementById("map_dash"),
            mapOptions);

        GPS.watch(function (position) {
            var pos = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);

            $.get("http://home.lanns.com.br:8070/monitor/get/"
                + position.coords.latitude + "/"
                + position.coords.longitude + "/10", function (monitors) {
                    monitors.forEach(function (item) {
                        var monitor = item.value;

                        var cityCircle = new google.maps.Circle({
                            strokeColor: colorFromDecibeis(monitor.Level),
                            strokeOpacity: 0.5,
                            strokeWeight: 2,
                            fillColor: colorFromDecibeis(monitor.Level),
                            fillOpacity: 0.1,
                            map: map,
                            title: monitor.SSID + " (" + monitor.Level + " dB)",
                            center: new google.maps.LatLng(monitor.Latitude, monitor.Longitude),
                            radius: monitor.Radius * 100,
                            zIndex: 9999 + monitor.Level
                        });

                        var marker = new google.maps.Marker({
                            anchorPoint: new google.maps.Point(-7, -7),
                            clickable: true,
                            position: new google.maps.LatLng(monitor.Latitude, monitor.Longitude),
                            map: map,
                            icon: iconFromDecibeis(monitor.Level),
                            title: monitor.SSID + " (" + monitor.Level + " dB)",
                            zIndex: 9999 + monitor.Level
                        });
                    });
                });

        }, function () {
        });

    }

    initialize();

})

.controller('ColaborateCtrl', function ($scope, GPS, Server, $ionicLoading, $ionicPopup) {
    var mapOptions = {
        center: new google.maps.LatLng(0, 0),
        zoom: 1,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    var map = new google.maps.Map(document.getElementById("map_colaborate"),
        mapOptions);

    var marker = new google.maps.Marker({
        position: new google.maps.LatLng(0, 0),
        zoom: 10,
        map: map,
        draggable: true
    });
    var markerDragged = false;

    google.maps.event.addListener(marker, 'dragend', function () {
        markerDragged = true;
    });

    $scope.accuracy = 0;
    GPS.watch(function (position) {
        var pos = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
        $(".accuracy").html(position.coords.accuracy);
        if (!markerDragged) {
            map.setZoom(19);
            map.setCenter(pos);
            marker.setPosition(pos);
        }
    }, function () {
    });

    $scope.syncWifi = function () {
        if (window.cordova) {
            $ionicLoading.show({
                template: 'Enviando...'
            });

            cordova.exec(function (accessPoints) {
                console.log(accessPoints);

                accessPoints.forEach(function (ap) {
                    ap.Latitude = marker.getPosition().lat();
                    ap.Longitude = marker.getPosition().lng();
                });

                Server.sendAccessPoints(accessPoints)
                    .then(function () {
                        window.location = "#/tab/dash";
                        $ionicLoading.hide();
                    }).catch(function () {

                        $ionicPopup.alert({
                            title: 'Oops!!',
                            template: 'Ocorreu um erro ao sincronizar com o servidor'
                        });

                        console.log(arguments);
                        $ionicLoading.hide();
                    });

            }, function (failureMessage) {

                $ionicPopup.alert({
                    title: 'Oops!!',
                    template: 'Ocorre um erro ao obter as redes wifi disponíveis'
                });

                console.log(failureMessage);
                $ionicLoading.hide();
            }, "WirelessManagerProxy", "scanWifi", []);
        } else {
            $ionicPopup.alert({
                title: 'Oops!!',
                template: 'A biblioteca do Cordova não está disponível!'
            });
        }
    };

});

function colorFromDecibeis(dBm) {
    // dBm to Quality:
    if (dBm <= -70)
        return '#FF0000';
    if (dBm >= -50)
        return '#00FF00';
    return '#7F7F00';
    //quality = 2 * (dBm + 100);
};

function iconFromDecibeis(dBm) {
    var path = "img/";
    if (dBm <= -70)
        return path + "Red.png";
    if (dBm >= -50)
        return path + "Green.png";
    return path + "Yellow.png";
};