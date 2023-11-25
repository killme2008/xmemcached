[![Build Status](https://travis-ci.org/killme2008/xmemcached.svg?branch=master)](https://travis-ci.org/killme2008/xmemcached)

## News

* [2.4.8](https://github.com/killme2008/xmemcached/releases/tag/xmemcached-2.4.8) released, some minor fixes.
* [2.4.7](https://github.com/killme2008/xmemcached/releases/tag/xmemcached-2.4.7) released, supports `MemcachedSessionComparator` and `resolveInetAddresses` settings and tweak benchmark projects.
* [2.4.6](https://github.com/killme2008/xmemcached/releases/tag/xmemcached-2.4.6) released, set timeoutExceptionThreshold though XMemcachedClientFactoryBean.

## Introduction

  XMemcached is a high performance, easy to use blocking multithreaded memcached client in java.

  It's nio based and was carefully turned to get top performance.

* [Homepage](http://fnil.net/xmemcached/)
* [Wiki](https://github.com/killme2008/xmemcached/wiki)
* [Javadoc](http://fnil.net/docs/xmemcached/index.html)
* [ChangeLog](https://github.com/killme2008/xmemcached/blob/master/NOTICE.txt)


Quick start:

* [Getting started](https://github.com/killme2008/xmemcached/wiki/Getting%20started)
* [快速入门](https://github.com/killme2008/xmemcached/wiki/%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8)

## Contribute

[Fork](https://github.com/killme2008/xmemcached#fork-destination-box) the source code and checkout it to your local machine. Make changes and create a pull request.

Use [docker](https://docs.docker.com/engine/installation/) and [docker-compose](https://docs.docker.com/compose/gettingstarted/) to setup test environment:

```sh
$ cd xmemcached
$ docker-compose up -d
```

Run unit tests:

```sh
$ mvn test
```

Run integration test:

```sh
$ mvn integration-test
```

Thanks to all contributors, you make xmemcached better.

## Contributors

* [cnscud](https://code.google.com/u/cnscud/)
* [wolfg1969](https://code.google.com/u/wolfg1969/)
* [vadimp](https://github.com/vadimp)
* [ilkinulas](https://github.com/ilkinulas)
* [aravind](https://github.com/aravind)
* [bmahe](https://github.com/bmahe)
* [jovanchohan](https://github.com/jovanchohan)
* [profondometer](https://github.com/profondometer)
* [machao9email](https://code.google.com/u/100914576372416966057)
* [spudone](https://github.com/spudone)
* [MikeBily](https://github.com/MikeBily)
* [Lucas Pouzac](https://github.com/lucaspouzac)
* [IluckySi](https://github.com/IluckySi)
* [saschat](https://github.com/saschat)
* [matejuh](https://github.com/matejuh)

## License

[Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
