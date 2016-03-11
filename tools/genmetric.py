#!/usr/bin/env python

import socket
import json
import random
import argparse


class Metric(object):
    def __init__(self, key='system', **attributes):
        self.key = key
        self.attributes = attributes

    def with_attrs(self, **attributes):
        return Metric(self.key, **dict(self.attributes.items() + attributes.items()))

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        pass

    def __call__(self, value, **attributes):
        return {'type': 'metric',
                'key': self.key,
                'value': value,
                'attributes': (dict(self.attributes.items() + attributes.items())
                               if attributes else self.attributes)}

class FFWDEmitter(object):
    def __init__(self, port=19000):
        self._socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self._addr = ('127.0.0.1', port)

    def emit(self, metric):
        self._socket.sendto(json.dumps(metric),
                            self._addr)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--what', type=str, default='foobar')
    parser.add_argument('--value', type=float, default=random.random() + 42)
    ns = parser.parse_args()

    metric = Metric('test', what=ns.what)
    emitter = FFWDEmitter()
    emitter.emit(metric=metric(ns.value))