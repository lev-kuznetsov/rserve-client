[![Build Status](https://travis-ci.org/lev-kuznetsov/rserve-client.svg?branch=master)](https://travis-ci.org/lev-kuznetsov/rserve-client) [![Coverage Status](https://coveralls.io/repos/github/lev-kuznetsov/rserve-client/badge.svg?branch=master)](https://coveralls.io/github/lev-kuznetsov/rserve-client?branch=master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/us.levk/rserve-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/us.levk/rserve-client)

# rserve-client

Java client for the [Rserve](https://rforge.net/Rserve/) server. This is an alternative to the client shipped by the package maintainers themselves. At the moment the client implements the websocket mode only, example Rserve server configuration can be found [here](src/test/resources/Rserv.conf). Check out some [examples](src/test/java/us/levk/rserve/client/E2e.java).
