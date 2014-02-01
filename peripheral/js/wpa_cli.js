var debug = require('debug')('wpa_cli');

var events = require('events');
var spawn = require('child_process').spawn;
var util = require('util');

var Wpa_cli = function() {
	var wpacli = 'wpa_cli';
	debug('wpa_cli = ' + wpacli);

	this._wpacli = spawn(wpacli);
	this._wpacli.on('close', this.onClose.bind(this));
	this._wpacli.stdout.on('data', this.onStdoutData.bind(this));
	this._wpacli.stderr.on('data', this.onStderrData.bind(this));

    this.disconnect();

	this._buffer = "";
}

util.inherits(Wpa_cli, events.EventEmitter);

Wpa_cli.prototype.kill = function() {
	this._wpacli.kill();
}

Wpa_cli.prototype.onClose = function(code) {
	debug('close = ' + code);
}

Wpa_cli.prototype.onStdoutData = function(data) {

	this._buffer += data.toString();
	debug('buffer = ' + JSON.stringify(this._buffer));
	var newLineIndex;

	while ((newLineIndex = this._buffer.indexOf('\n')) !== -1) {
		var line = this._buffer.substring(0, newLineIndex);
		var found;

		this._buffer = this._buffer.substring(newLineIndex + 1);

		debug('line = ' + line);

		// Connected
		if ((found = line.match(/CTRL-EVENT-CONNECTED/g))) {
			console.log("FOUND IT!");
			this.emit('connected', found[0]);
		}
	}
}

Wpa_cli.prototype.onStderrData = function(data) {
	console.error('stderr: ' + data);
}

Wpa_cli.prototype.sendCommand = function(command) {
	this._wpacli.stdin.write(command.toString() + '\n');
}

Wpa_cli.prototype.connect = function(ssid, psk) {
	// We guess that we will get id 0 :-S
	this._wpacli.stdin.write("add_network\n");
	this._wpacli.stdin.write("set_network 0 ssid \"" + ssid + "\"\n");

	if (/\S/.test(psk)) { // string is not empty and not just whitespace
		this._wpacli.stdin.write("set_network 0 psk \"" + psk + "\"\n");
	} else {
		// assume this is an open network
		this._wpacli.stdin.write("set_network 0 key_mgmt NONE\n");
	}

	this._wpacli.stdin.write("select_network 0\n");
}

Wpa_cli.prototype.disconnect = function() {
	this._wpacli.stdin.write("disconnect\n");
	// Disconnect and remove network with id 0 through id 9
    for (var x = 0; x < 10; x ++) {
	    this._wpacli.stdin.write("remove_network " + x +"\n");
    }
}

module.exports = Wpa_cli;

