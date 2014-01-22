var debug = require('debug')('dhcp_cli');

var events = require('events');
var spawn = require('child_process').spawn;
var util = require('util');

var command = 'dhclient';
var args = ['wlan0', '-v'];

var Dhcp_cli = function() {
	this._dhcpcli;
	debug('dhcp_cli = ' + this._dhcpclient);
}

util.inherits(Dhcp_cli, events.EventEmitter);

Dhcp_cli.prototype.onStdoutData = function(data) {
	console.log(data.toString());
}
Dhcp_cli.prototype.onStderrData = function(data) {
	// Parse the output on std error
	this._buffer += data.toString();
	debug('buffer = ' + JSON.stringify(this._buffer));

	var newLineIndex;
	while ((newLineIndex = this._buffer.indexOf('\n')) !== -1) {
		var line = this._buffer.substring(0, newLineIndex);
		var found;
	 	
		this._buffer = this._buffer.substring(newLineIndex + 1);
		debug('line = ' + line);
	  		// Connected
		if ((found = line.match(/bound/g))) {
			this.emit('connected', found[0]);
  		}
	}
}

Dhcp_cli.prototype.getIp = function() {
  	this._buffer = "";
	this._dhcpcli = spawn(command, args);
	this._dhcpcli.stderr.on('data', this.onStderrData.bind(this));
	this._dhcpcli.stdout.on('data', this.onStdoutData.bind(this));
}

module.exports = Dhcp_cli;

