var http = require('http');
var port = process.env.port || 1337;
var odataPort = process.env.odataPort || 52999;

require('odata-server');
require('jaydata-mongodb-pro');

var express = require('express');
//require('jaydata');
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

var bodyParser = require('body-parser');
var cookieSession = require('cookie-session');

// parse various different custom JSON types as JSON
//app.use(bodyParser.json({ type: 'application/*+json' }));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
    extended: true
}));

app.use(cookieSession({
    keys: ['secret1', 'secret2']
}));

//require('qs');
//require('cookie-parser');
//require('method-override');
//require('express-session');

//app.use(express.query());
//app.use(express.methodOverride());
//app.use(express.session({ secret: 'session key' }));
//app.use(express.errorHandler());

var connectToDb = function () {
    return new WifiIntensityDatabase({ name: "mongoDB", databaseName: "WifiIntensityDatabase", address: "localhost", port: 27017 });
};

app.use("/widb", $data.JayService.OData.Utils.simpleBodyReader());

$data.createODataServer(WifiIntensityDatabase, '/widb.svc', odataPort, 'localhost');

app.use("/", express.static(__dirname));

app.get("/monitor/get/:lat/:long", function (req, res) {
    
    var latitude = req.param('lat');
    var longitude = req.param('long');
    
    connectToDb().onReady(function (db) {
        
        db.AccessPointMonitors.aggregate({
            $group : {
                _id : "$BSSID",
                Total : { $sum : 1 }
            }
        }).toArray(function (records) {
            res.writeHead(200, {
                'Content-Type': 'application/json'
            });
            res.write(JSON.stringify(records));
            res.end();
        });
        

    });
    
});

app.post("/monitor/batch", function (req, res) {
    
    connectToDb().onReady(function (db) {
        
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

app.listen(8080);