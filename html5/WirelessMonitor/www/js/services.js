angular.module('starter.services', [])

.factory('GPS', function () {
    return {
        watch: function (watchFn, watchError) {
            var geolocationOptions = {};
            var bestLocation = null;
            var bestLocationTime = new Date();
            var TIME_DIFFERENCE_THRESHOLD = 1 * 60 * 1000;

            var isBetterLocation = function (oldLocation, newLocation) {
                // If there is no old location, of course the new location is better.
                if (oldLocation == null)
                    return true;

                newLocation.time = (new Date()).valueOf();

                // Check if new location is newer in time.
                var isNewer = newLocation.time > oldLocation.time;
                // Check if new location more accurate. Accuracy is radius in meters, so less is better.
                var isMoreAccurate = newLocation.accuracy < oldLocation.accuracy;

                if (isMoreAccurate && isNewer) {
                    // More accurate and newer is always better.         
                    return true;
                } else if (isMoreAccurate && !isNewer) {
                    // More accurate but not newer can lead to bad fix because of user movement.         
                    // Let us set a threshold for the maximum tolerance of time difference.         
                    var timeDifference = newLocation.time - oldLocation.time;

                    // If time difference is not greater then allowed threshold we accept it.         
                    if (timeDifference > -TIME_DIFFERENCE_THRESHOLD) {
                        return true;
                    }
                }

                return false;
            }

            navigator.geolocation.watchPosition(function (position) {
                if (isBetterLocation(bestLocation ? bestLocation.coords : null, position.coords)) {
                    bestLocation = position;
                    watchFn(bestLocation);
                }
            }, watchError, geolocationOptions);
        }
    };
})

/**
 * A simple example service that returns some data.
 */
.factory('Server', function ($q) {
    return {
        sendAccessPoints: function (accessPoints) {
            var deferred = $q.defer();

            $.ajax({
                url: "http://home.lanns.com.br:8070/monitor/batch/",
                type: 'POST',
                dataType: "json",
                data: JSON.stringify(accessPoints),
                contentType: "application/json",
                success: function (response) {
                    deferred.resolve(response);
                },
                error: function (failureMessage) {
                    deferred.reject(failureMessage);
                }
            });

            return deferred.promise;
        }
    }
});
