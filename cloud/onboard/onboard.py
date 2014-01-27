# vim:set ts=4 sw=4:
#
# Copyright (c) 2012 cozybit Inc.
#

import re
import logging
import os
import datetime

from google.appengine.ext import ndb
from google.appengine.api import namespace_manager
from google.appengine.api import channel

import webapp2
import jinja2

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)

def fuzzy_readable_time(delta):
    if not delta:
        return "never"
    if (delta.total_seconds() < 1):
        return "just now"
    if (delta.total_seconds() < 2):
        return str(delta.seconds) + " second ago"
    if (delta.total_seconds() < 60):
        return str(delta.seconds) + " seconds ago"
    elif delta.total_seconds() < 120:
        return str(delta.seconds / 60) + " minute ago"
    elif delta.total_seconds() < 3600:
        return str(delta.seconds / 60) + " minutes ago"
    elif delta.total_seconds() < 2 * 3600:
        return str(delta.seconds / 3600) + " hour ago"
    elif delta.total_seconds() < 24 * 3600:
        return str(delta.seconds / 3600) + " hours ago"
    elif delta.total_seconds() < 2 * 24 * 3600:
        return str(delta.days) + " day ago"
    else:
        return str(delta.days) + " days ago"

def validate_did(did):
        """A did has the format of a mac address XX:XX:XX:XX:XX:XX"""
        return re.match(r'([0-9A-F]{2}[:-]){5}([0-9A-F]{2})', did, re.I) != None

def validate_status(status):
        return status == "on" or status == "off"

class DeviceNode(ndb.Model):
    """Models onboardee device"""
    last_seen = ndb.DateTimeProperty(auto_now=True)
    status = ndb.StringProperty();
    avg_energy = ndb.IntegerProperty(default=0);

class CheckIn(webapp2.RequestHandler):
    def get(self):
        # Maybe some tiny embedded nodes don't support post so accept
        # GET checkins as well.
        self.post()

    def post(self):
        vendorid = self.request.str_params['vendorid']
        logging.info('vendorid: ' + vendorid)
        try:
            namespace_manager.set_namespace(vendorid)
        except:
            self.error(400);
            return

        deviceid = self.request.str_params['deviceid'].lower()
        logging.info('deviceid: ' + deviceid)
        if not validate_did(deviceid):
            self.error(400)
            return

        status = self.request.str_params['status'].lower()
        logging.info('status: ' + status)
        if not validate_status(status):
            self.error(400)
            return

        node = DeviceNode.get_by_id(id=deviceid)
        if (node == None):
            node = DeviceNode(id=deviceid)
            logging.info('new DeviceNode: ' + deviceid)

        if 'reset_energy' in self.request.arguments():
                node.avg_energy = 0

        if node.status != status:
            node.status = status
            clientid = vendorid + deviceid
            if status == 'on':
                node.avg_energy += 200
                channel.send_message(clientid, str(node.avg_energy))
            else:
                channel.send_message(clientid, "off")

        node.put()

        return

class MainPage(webapp2.RequestHandler):

    def get(self):
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.write('TBD')

class ChannelStatus(webapp2.RequestHandler):
    def post(self):
        clientid = self.request.get('from')
        action = self.request.path.split('/')[-2]
        logging.info('clientid ' + clientid + ' is now ' + action)

class StatusPage(webapp2.RequestHandler):
    def get(self, vendorid, deviceid):
        try:
            namespace_manager.set_namespace(vendorid)
        except:
            self.error(400)
            return

        if not validate_did(deviceid):
            self.error(400)
            return

        node = DeviceNode.get_by_id(deviceid)
        last_update = None
        if node is not None:
            last_update = datetime.datetime.now() - node.last_seen
        logging.info('last_update: ' + str(last_update))

        clientid = vendorid + deviceid
        token = channel.create_channel(clientid)
        # logging.info('token ' + token)

        template_values = {
            'token': token,
            'vendorid': vendorid,
            'deviceid': deviceid,
            'node': node,
            'last_update': fuzzy_readable_time(last_update),
        }

        template = JINJA_ENVIRONMENT.get_template('index.html')
        self.response.write(template.render(template_values))

        return


application = webapp2.WSGIApplication([
    ('/', MainPage),
    ('/checkin', CheckIn),
    ('/status/([-\w]+)/([-:\w]+)', StatusPage),
    ('/_ah/channel/connected/', ChannelStatus),
    ('/_ah/channel/disconnected/', ChannelStatus),
], debug=True)

