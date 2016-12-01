var http = require('http');
var port = process.env.port || 1337;
var odataPort = process.env.odataPort || 52999;

require('odata-server');
require('jaydata-mongodb-pro');

var mongojs = require('mongojs');
var db = mongojs('mongodb://localhost:27017/WifiIntensityDatabase', ['AccessPoints', 'AccessPointMonitors', 'Areas']);

var express = require('express');
window.DOMParser = require('xmldom').DOMParser;
require('q');

$data.Class.define("AccessPoint", $data.Entity, null, {
    Id: { type: "Id", key: true, computed: true },
    CreateDate: { type: Date },
    BSSID: { type: String, required: true, maxLength: 200 },
    Monitors: { type: "Array", elementType: "AccessPointMonitor", inverseProperty: "AccessPoint" }
}, null);

$data.Class.define("AccessPointMonitor", $data.Entity, null, {
    Id: { type: "Id", key: true, computed: true },
    CreateDate: { type: Date },
    BSSID: { type: String, required: true, maxLength: 200 },
    SSID: { type: String, required: true, maxLength: 200 },
    Capabilities: { type: String, required: true, maxLength: 200 },
    Level: { type: Number, required: true },
    Frequency: { type: Number, required: true },
    Timestamp: { type: Number },
    Position: { type: $data.GeographyPoint },
    AccessPoint: { type: "AccessPoint", inverseProperty: "Monitors" }
}, null);

$data.Class.define("WifiIntensityDatabase", $data.EntityContext, null, {
    AccessPoints: { type: $data.EntitySet, elementType: AccessPoint },
    AccessPointMonitors: { type: $data.EntitySet, elementType: AccessPointMonitor }
}, null);

var app = express();

(function () {
    var bodyParser = require('body-parser');
    var cookieSession = require('cookie-session');
    var allowCrossDomain = function (req, res, next) {
        res.header('Access-Control-Allow-Origin', '*');
        res.header('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE');
        res.header('Access-Control-Allow-Headers', 'Content-Type');
        
        next();
    }
    
    app.use(bodyParser.json());
    app.use(bodyParser.urlencoded({
        extended: true
    }));
    
    app.use(cookieSession({
        keys: ['secret1', 'secret2']
    }));
    app.use(allowCrossDomain);
    //app.use(app.router);
    
    app.use("/widb", $data.JayService.OData.Utils.simpleBodyReader());
    
    $data.createODataServer(WifiIntensityDatabase, '/widb.svc', odataPort, 'localhost');
    
    app.use("/", express.static(__dirname));
    
    app.use(function (req, res, next) {
        //req.db = db;
        next();
    });
})();

app.get("/monitor/get/:lat/:long/:radius", function (req, res) {
    var latitude = parseFloat(req.param('lat'));
    var longitude = parseFloat(req.param('long'));
    var radius = parseFloat(req.param('radius')) + 2;
    
    if (radius == null || radius > 20000 || radius <= 0)
        radius = 1;
    
    var mapper = function () {
        emit(this.BSSID, {
            BSSID: this.BSSID,
            SSID: this.SSID,
            Latitude: this.Position[1] * this.Level,
            Longitude: this.Position[0] * this.Level,
            Level: this.Level,
            Count: 1,
            Radius: 0
        });
    };
    
    var reducer = function (keys, values) {
        var distance = function (lat1, lon1, lat2, lon2) {
            var R = 6371; // km
            var PI = 3.14;
            var q1 = lat1 * PI / 180;
            var q2 = lat2 * PI / 180;
            var tq = (lat2 - lat1) * PI / 180;
            var tl = (lon2 - lon1) * PI / 180;
            
            var a = Math.sin(tq / 2) * Math.sin(tq / 2) +
		        Math.cos(q1) * Math.cos(q2) *
		        Math.sin(tl / 2) * Math.sin(tl / 2);
            var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            
            return R * c;
        };
        
        var reduced = { BSSID: "", SSID: "", Latitude: 0, Longitude: 0, Level: 0, Count: 0, Radius: 0 };
        
        reduced.Count = 0;
        reduced.BSSID = values[0] ? values[0].BSSID : "";
        reduced.SSID = values[0] ? values[0].SSID : "";
        
        var firstPoint = { Latitude: 0, Longitude: 0 };
        
        for (var idx = 0; idx < values.length; idx++) {
            reduced.Latitude += values[idx].Latitude;
            reduced.Longitude += values[idx].Longitude;
            reduced.Level += values[idx].Level;
            
            if (idx > 0) {
                //reduced.Radius = Math.sqrt((lastPoint.Latitude - values[idx].Latitude) * (lastPoint.Latitude - values[idx].Latitude) 
                //    + (lastPoint.Longitude - values[idx].Longitude) * (lastPoint.Longitude - values[idx].Longitude)) / 6371;
                
                reduced.Radius = Math.max(reduced.Radius,				
					Math.sqrt((firstPoint.Latitude - values[idx].Latitude) * (firstPoint.Latitude - values[idx].Latitude) 
                    + (firstPoint.Longitude - values[idx].Longitude) * (firstPoint.Longitude - values[idx].Longitude)) / 6371);
				
				//reduced.Radius = Math.max(reduced.Radius,				
				//	distance(firstPoint.Latitude, firstPoint.Longitude, 
				//		values[idx].Latitude, values[idx].Longitude));
            } else {
                firstPoint.Latitude = values[idx].Latitude;
                firstPoint.Longitude = values[idx].Longitude;
            }
            
            reduced.Count++;
        }
        
        return reduced;
    };
    
    var finalized = function (key, reduced) {
        reduced.Latitude = reduced.Latitude / reduced.Level;
        reduced.Longitude = reduced.Longitude / reduced.Level;
        reduced.Level = reduced.Level / reduced.Count;
        return reduced;
    }
    
    db.AccessPointMonitors.mapReduce(mapper, reducer, {
        finalize: finalized,
        query: {
            Position: { $within: { $center: [ [ longitude, latitude ], radius / 6371 ] } }
        },
        keeptemp: false,
        out: "Areas"
    }, function (err, areas) {
        db.Areas.find(function (err, docs) {
            if (err) console.log(err);
            console.log("\n", docs);
            
            res.writeHead(200, {
                'Content-Type': 'application/json'
            });
            res.write(JSON.stringify(docs));
            res.end();

        });
    });

});

app.post("/monitor/batch", function (req, res) {
    
    var db = new WifiIntensityDatabase({
        name: "mongoDB",
        databaseName: "WifiIntensityDatabase", 
        address: "localhost", 
        port: 27017
    });
    
    db.onReady(function (db) {
        
        var accessPoints = [];
        var monitors = req.body;
        
        for (var i = 0; i < monitors.length; i++) {
            var monitor = monitors[i];
            db.AccessPointMonitors.add(new AccessPointMonitor({
                CreateDate: new Date(),
                BSSID: monitor.BSSID,
                Position: new $data.GeographyPoint(monitor.Longitude, monitor.Latitude),
                SSID: monitor.SSID,
                Frequency: monitor.Frequency,
                Level: monitor.Level,
                Capabilities: monitor.Capabilities
            }));
            
            if (accessPoints.indexOf(monitor.BSSID) == -1)
                accessPoints.push(monitor.BSSID);
        }
        
        db.saveChanges()
                    .then(function (scRes) {
            console.log(monitors.length + " monitors cadastrados");
            
            res.writeHead(200, {
                'Content-Type': 'application/json'
            });
            res.write(JSON.stringify("OK"));
            res.end();
            
            accessPoints.forEach(function (bssid) {
                db.AccessPoints.filter(function (ap) { return ap.BSSID == this.BSSID; }, { BSSID : bssid })
                .toArray(function (accessPoints) {
                    var accessPoint = accessPoints[0];
                    
                    if (!accessPoint) {
                        accessPoint = new AccessPoint({
                            CreateDate: new Date(),
                            BSSID: bssid
                        });
                        db.AccessPoints.add(accessPoint);
                    }
                    
                    db.saveChanges()
                    .then(function (scRes) {
                        console.log(scRes);
                    }).fail(function (scFail) {
                        console.log(scRail);
                    });
                });
            });

        }).fail(function (scFail) {
            console.log(scRail);
            
            res.writeHead(500, {
                'Content-Type': 'application/json'
            });
            res.end();
        });

    });

});

app.listen(8070);