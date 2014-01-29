var util = require('util');

var bleno = require(process.env.ONBOARD_NODE_MODULES + '/bleno/index');
var Wpa_cli = require('./wpa_cli');
var Dhcp_cli = require('./dhcp_cli');
var events = require('events');

var BlenoPrimaryService = bleno.PrimaryService;
var BlenoCharacteristic = bleno.Characteristic;
var BlenoDescriptor = bleno.Descriptor;

/* Define external cli bindings object */

var CliBindings = function() {
	this._wpa_cli = new Wpa_cli();
	this._dhcp_cli = new Dhcp_cli();
}

util.inherits(CliBindings, events.EventEmitter);

var cliBindings = new CliBindings();

cliBindings.init = function() {
	this._wpa_cli.on('connected', this.onWpaConnected.bind(this));
	this._dhcp_cli.on('connected', this.onDhcpConnected.bind(this));
}

cliBindings.onWpaConnected = function (ssid) {
	wlan_link = true;
	getStatus();
	cliBindings._dhcp_cli.getIp();
}

cliBindings.onDhcpConnected = function (ssid) {
	ip_addr = true;
	getStatus();
	// At this point we got internet...
	checkin();
}


/* End of cli bindings object */

cliBindings.init();

function checkin () {

var http = require('http');
var querystring = require('querystring');

var post_data = querystring.stringify({
      'vendorid' : 'toastmaster',
      'deviceid': '00:11:22:22:11:00',
      'status': 'on'
  });


var options = {
  hostname: 'cozyonboard.appspot.com',
  port: 80,
  path: '/checkin',
  method: 'POST'
};

var req = http.request(options, function(res) {
  console.log('STATUS: ' + res.statusCode);
  console.log('HEADERS: ' + JSON.stringify(res.headers));
  res.setEncoding('utf8');
  res.on('data', function (chunk) {
    console.log('BODY: ' + chunk);
  });
});

req.on('error', function(e) {
  console.log('problem with request: ' + e.message);
});

// write data to request body
req.write(post_data);
req.end();
}


console.log('OnBoarding bleno app');

/* Global variables */

var statuses = {
    DISCONNECTED: { id: 0, data: "DISCONNECTED" },
    INITIALIZING: { id: 1, data: "INITIALIZING" },
    INITIALIZED : { id: 2, data: "INITIALIZED" },
    CONNECTING  : { id: 3, data: "CONNECTING" },
    CONNECTED   : { id: 4, data: "CONNECTED" },
    FAILED      : { id: 5, data: "FAILED" },
};

var messages = {
    NONE       : { id: 0, data: " " },
    SET_SSID   : { id: 1, data: "Set SSID" },
    SET_AUTH   : { id: 2, data: "Set authentication type"},
    WRONG_AUTH : { id: 3, data: "Wrong authentication type selected" },
    SET_PSK    : { id: 4, data: "Set passphrase" },
    SET_CHAN   : { id: 5, data: "Set channel or issue CONNECT cmd" },
    AUTHING    : { id: 6, data: "Authenticating"},
    GETTING_IP : { id: 7, data: "Getting IP address"},
    CONN_ESTAB : { id: 8, data: "Connection established"},
}

var theStatus = [statuses.DISCONNECTED];
var theDetails = [messages.NONE];

var ssid = '';
var authentication = 'OPEN';
var passphrase = '';
var channel = 0;
var wlan_link = false;
var ip_addr = false;

/* Auth type List */

var AUTH_OPEN = "OPEN";
var AUTH_WEP = "WEP";
var AUTH_WPA = "WPA";

/* Command List */

var CMD_CONNECT = 0x01;
var CMD_DISCONNECT = 0x02;
var CMD_RESET = 0x03;

/* Store ptr to Status Callbacks */

var StatusCallback;
var DetailedStatusCallback;

/*
 * State transition
 */

function updateStatus(_status,_detailed) {
	theStatus[0] = _status;
	theDetails[0] = _detailed;

	console.log("Updated status to: " + theStatus[0].data + " detailed: " + theDetails[0].data);
	var data = new Buffer(theStatus[0].data.length);
	data.write(theStatus[0].data, 0);
	if (StatusCallback != null)
		StatusCallback(data);
	// TODO Enable this at some point
	//if (DetailedStatusCallback != null)
	//	DetailedStatusCallback(data);
}

/*
 * Get current status function, this function
 * should be called after each attr write
 */

function getStatus() {

	/* We only check the field values when initializing */
	if (theStatus[0].id < statuses.CONNECTING.id) {
		if (ssid.length == 0) {
			updateStatus(statuses.INITIALIZING, messages.SET_SSID);
			return;
		}

		if (authentication.length == 0) {
			updateStatus(statuses.INITIALIZING, messages.SET_AUTH);
			return;
		}

		switch (authentication) {
			case AUTH_OPEN:
				break;
			case AUTH_WEP:
			case AUTH_WPA:
				if (passphrase.length == 0) {
			        updateStatus(statuses.INITIALIZING, messages.SET_PSK);
					return;
				}
				break;
			default:
			    updateStatus(statuses.INITIALIZING, messages.WRONG_AUTH);
				return;
		}

		/*
		 * If we arrive here all mandatory attr have been set
		 * Inform the user that is possible to set the channel
		 */
		updateStatus(statuses.INITIALIZED, messages.SET_CHAN);
	}

	/* CONNECTING */
	if (theStatus[0].id == statuses.CONNECTING.id) {
		if (!wlan_link) {
			updateStatus(statuses.CONNECTING, messages.AUTHING);
			return;
		}

		if (!ip_addr) {
			updateStatus(statuses.CONNECTING, messages.GETTING_IP);
			return;
		}

		/* We are connected to the AP */
		updateStatus(statuses.CONNECTED, messages.CONN_ESTAB);
	}
}

/* Connect function */

function connect() {

	if (theStatus[0].id == statuses.INITIALIZED.id) {
		// TODO
		// Do whatever needed to initialize connection...
		// For instance contact wpa_supplicant
		updateStatus(statuses.CONNECTING, messages.NONE);
		cliBindings._wpa_cli.connect(ssid, passphrase);
	}
}

/* Disconnect function */

function disconnect() {
	if (theStatus[0].id > statuses.INITIALIZED.id) {
        // TODO Disconnect
		updateStatus(statuses.DISCONNECTED, messages.NONE);
	}
}

/* Reset function */

function reset() {
	ssid = '';
	authentication = '';
	passphrase = '';
	channel = 0;
}
/*
 * Status Characteristic
 */

var StatusCharacteristic = function() {
	StatusCharacteristic.super_.call(this, {
 		uuid: 'FFFFFFFFC0C1FFFFC0C1201401000001',
		properties: ['notify', 'read']
	});
};

util.inherits(StatusCharacteristic, BlenoCharacteristic);

StatusCharacteristic.prototype.onSubscribe = function(maxValueSize, updateValueCallback) {

	console.log('Subscribed to StatusCharacteristic ');
	StatusCallback = updateValueCallback;
	getStatus();
};

StatusCharacteristic.prototype.onUnsubscribe = function() {
	console.log('Unsubscribed from StatusCharacteristic');
	StatusCallback = null;
};

StatusCharacteristic.prototype.onNotify = function() {
	console.log('Notified StatusCharacteristic');
};

StatusCharacteristic.prototype.onReadRequest = function(offset, callback) {

	var result = this.RESULT_SUCCESS;

	var stat = theStatus[0].data;
	var data = new Buffer(stat.length);

	data.write(stat);

	console.log("Read StatusCharacteristic: " + stat);

	// NO IDEA OF WHAT THIS CHECK IS USEFUL FOR...
	if (offset > data.length) {
		result = this.RESULT_INVALID_OFFSET;
		data = null;
	}

	callback(result, data);
}


/*
 * DetailedStatus Characteristic
 */

var DetailedStatusCharacteristic = function() {
	DetailedStatusCharacteristic.super_.call(this, {
 		uuid: 'FFFFFFFFC0C1FFFFC0C1201401000002',
		properties: ['notify']
	});
};

util.inherits(DetailedStatusCharacteristic, BlenoCharacteristic);

DetailedStatusCharacteristic.prototype.onSubscribe = function(maxValueSize, updateValueCallback) {

	console.log('Subscribed to DetailedStatusCharacteristic ');
	DetailedStatusCallback = updateValueCallback;
	getStatus();
};

DetailedStatusCharacteristic.prototype.onUnsubscribe = function() {
	console.log('Unsubscribed from DetailedStatusCharacteristic');
	DetailedStatusCallback = null;
};

DetailedStatusCharacteristic.prototype.onNotify = function() {
	console.log('Notified DetailedStatusCharacteristic');
};

/*
 * SSID Characteristic
 */
var SSIDCharacteristic = function() {
	SSIDCharacteristic.super_.call(this, {
		uuid: 'FFFFFFFFC0C1FFFFC0C1201401000003',
		properties: ['read','write'],
	});
};

util.inherits(SSIDCharacteristic, BlenoCharacteristic);

SSIDCharacteristic.prototype.onWriteRequest = function(data, offset, withoutResponse, callback) {

	ssid = data.toString();
	console.log("Wrote SSIDCharacteristic ("+ data.length +"): " + ssid);
	callback(this.RESULT_SUCCESS);
	getStatus();
};

SSIDCharacteristic.prototype.onReadRequest = function(offset, callback) {
	var result = this.RESULT_SUCCESS;
	var data = new Buffer(ssid.length);

	data.write(ssid);

	console.log("Read SSIDCharacteristic");

	// NO IDEA OF WHAT THIS CHECK IS USEFUL FOR...
	if (offset > data.length) {
		result = this.RESULT_INVALID_OFFSET;
		data = null;
	}
	callback(result, data);
}

/*
 * Authentication Characteristic
 */

var AuthenticationCharacteristic = function() {
	AuthenticationCharacteristic.super_.call(this, {
		uuid: 'FFFFFFFFC0C1FFFFC0C1201401000004',
		properties: ['write'],
	});
};

util.inherits(AuthenticationCharacteristic, BlenoCharacteristic);

AuthenticationCharacteristic.prototype.onWriteRequest = function(data, offset, withoutResponse, callback) {

	authentication = data.toString();
	console.log("Wrote AuthenticationCharacteristic: " + authentication);
	callback(this.RESULT_SUCCESS);
	getStatus();
};

/*
 * Passphrase Characteristic
 */

var PassphraseCharacteristic = function() {
	PassphraseCharacteristic.super_.call(this, {
		uuid: 'FFFFFFFFC0C1FFFFC0C1201401000005',
		properties: ['write'],
	});
};

util.inherits(PassphraseCharacteristic, BlenoCharacteristic);

PassphraseCharacteristic.prototype.onWriteRequest = function(data, offset, withoutResponse, callback) {

	passphrase = data.toString();
	console.log("Wrote PassphraseCharacteristic:" + passphrase);
	callback(this.RESULT_SUCCESS);
	getStatus();
};

/*
 * Channel Characteristic
 */
var ChannelCharacteristic = function() {
	ChannelCharacteristic.super_.call(this, {
		uuid: 'FFFFFFFFC0C1FFFFC0C1201401000006',
		properties: ['read','write'],
	});
};

util.inherits(ChannelCharacteristic, BlenoCharacteristic);

ChannelCharacteristic.prototype.onWriteRequest = function(data, offset, withoutResponse, callback) {

	channel = data.toString();
	console.log("Wrote ChannelCharacteristic:" + channel);
	callback(this.RESULT_SUCCESS);
	getStatus();
};

ChannelCharacteristic.prototype.onReadRequest = function(offset, callback) {
	var result = this.RESULT_SUCCESS;
	var data = new Buffer(ssid.length);

	data.write(channel);

	console.log("Read ChannelCharacteristic");

	// NO IDEA OF WHAT THIS CHECK IS USEFUL FOR...
	if (offset > data.length) {
		result = this.RESULT_INVALID_OFFSET;
		data = null;
	}
	callback(result, data);
}

/*
 * Command Characteristic
 */

var CommandCharacteristic = function() {
	CommandCharacteristic.super_.call(this, {
		uuid: 'FFFFFFFFC0C1FFFFC0C1201401000007',
		properties: ['write'],
	});
};

util.inherits(CommandCharacteristic, BlenoCharacteristic);

CommandCharacteristic.prototype.onWriteRequest = function(data, offset, withoutResponse, callback) {
	console.log("Wrote CommandCharacteristic: " + data);

	var r = data.readUInt8(0);
	var result = this.RESULT_SUCCESS;

	// Here we should check which command is
	// Depending on the command execute the
	// necessary function

	switch(r) {
		case CMD_CONNECT:
			connect(ssid, passphrase);
			break;
		case CMD_DISCONNECT:
			disconnect();
			break;
		case CMD_RESET:
			reset();
			break;
		default:
			result = this.RESULT_UNLIKELY_ERROR;
			console.log("Written Unknown command: " + data);
	}


	callback(result);
	getStatus();
};


/*
 * OnBoarding Service
 */

function OnBoardingService() {
	OnBoardingService.super_.call(this, {
		uuid: 'FFFFFFFFC0C1FFFFC0C1201401000000',
		characteristics: [
			new StatusCharacteristic(),
			new DetailedStatusCharacteristic(),
			new SSIDCharacteristic(),
			new AuthenticationCharacteristic(),
			new PassphraseCharacteristic(),
			new ChannelCharacteristic(),
			new CommandCharacteristic()
    		]
	});
}

util.inherits(OnBoardingService, BlenoPrimaryService);

bleno.on('stateChange', function(state) {
  console.log('on -> stateChange: ' + state);

  if (state === 'poweredOn') {
    bleno.startAdvertising('JloDev', ['FFFFFFFFC0C1FFFFC0C1201401000000']);
  } else {
    bleno.stopAdvertising();
  }
});

bleno.on('advertisingStart', function(error) {
  console.log('on -> advertisingStart: ' + (error ? 'error ' + error : 'success'));

  if (!error) {
    bleno.setServices([
      new OnBoardingService()
    ]);
  }
});

bleno.on('advertisingStop', function() {
  console.log('on -> advertisingStop');
});

bleno.on('servicesSet', function() {
  console.log('on -> servicesSet');
});
