# Caffeine Service Broker

Service Broker for [Caffeine as a Service](https://github.com/making/caffeine-service).

### Deploy Service Broker

``` console
$ ./mvnw clean package
$ cf push -b java_buildpack --no-start
$ cf set-env CAFFEINE_SERVICE_URI http://caffeine-service.local.pcfdev.io
$ cf set-env CAFFEINE_SERVICE_USERNAME master
$ cf set-env CAFFEINE_SERVICE_PASSWORD master
$ cf set-env SECURITY_USER_NAME demo
$ cf set-env SECURITY_USER_PASSWORD demo
$ cf start caffeine-broker
```

`cf apps` will return as follows

``` console
$ cf a
Getting apps in org pcfdev-org / space pcfdev-space as admin...
OK

name               requested state   instances   memory   disk   urls
caffeine-service   started           1/1         512M     1G     caffeine-service.local.pcfdev.io
caffeine-broker    started           1/1         512M     1G     caffeine-broker.local.pcfdev.io
```

### Register Service Broker

``` console
$ cf create-service-broker p-caffeine demo demo http://caffeine-broker.local.pcfdev.io
$ cf enable-service-access p-caffeine
```

`cf service-brokers` will return

``` console
$ cf service-brokers
Getting service brokers as admin...

name         url
p-caffeine   http://caffeine-broker.local.pcfdev.io <<--- Added
p-mysql      http://mysql-broker.local.pcfdev.io
p-rabbitmq   http://rabbitmq-broker.local.pcfdev.io
p-redis      http://redis-broker.local.pcfdev.io
```

`cf marketplace` will return

``` console
$ cf m
Getting services from marketplace in org pcfdev-org / space pcfdev-space as admin...
OK

service      plans          description
p-caffeine   strong, weak   A caffeine service broker <<--- Added
p-mysql      512mb, 1gb     MySQL databases on demand
p-rabbitmq   standard       RabbitMQ is a robust and scalable high-performance multi-protocol messaging broker.
p-redis      shared-vm      Redis service to provide a key-value store

TIP:  Use 'cf marketplace -s SERVICE' to view descriptions of individual plans of a given service.
```

### Create and Bind Service

``` console
$ cf create-service p-caffeine strong my-caffeine
$ cf bind-service some-app my-caffeine
```

You can see caffeine-service's credentials by `cf env`

``` console
$ cf env some-app
...

System-Provided:
 "VCAP_SERVICES": {
  "p-caffeine": [
   {
    "credentials": {
     "password": "8fbe73dc-ea68-4ad4-b01b-6068ed3e22ec",
     "uri": "http://caffeine-service.local2.pcfdev.io/caffeine/e99e2910-6122-4dbf-a204-8297bb6e28a4",
     "username": "d8b62608-ee1a-4978-a911-cf0c20a67b80"
    },
    "label": "p-caffeine",
    "name": "my-caffeine",
    "plan": "strong",
    "provider": null,
    "syslog_drain_url": null,
    "tags": [
     "caffeine"
    ]
   }
  ]
 }
}

...
```

You can access caffeine-service using this credentials

``` console
$ curl -v -XPUT -u d8b62608-ee1a-4978-a911-cf0c20a67b80:8fbe73dc-ea68-4ad4-b01b-6068ed3e22ec http://caffeine-service.local.pcfdev.io/caffeine/e99e2910-6122-4dbf-a204-8297bb6e28a4/foo \
       -H 'Content-Type: text/plain' \
       -d Hello
* Server auth using Basic with user 'd8b62608-ee1a-4978-a911-cf0c20a67b80'
> PUT /caffeine/e99e2910-6122-4dbf-a204-8297bb6e28a4/foo HTTP/1.1
> Host: caffeine-service.local2.pcfdev.io
> Authorization: Basic ZDhiNjI2MDgtZWUxYS00OTc4LWE5MTEtY2YwYzIwYTY3YjgwOjhmYmU3M2RjLWVhNjgtNGFkNC1iMDFiLTYwNjhlZDNlMjJlYw==
> User-Agent: curl/7.43.0
> Accept: */*
> Content-Type: text/plain
> Content-Length: 5
>
< HTTP/1.1 201 Created
< Content-Length: 3
< Content-Type: text/plain;charset=UTF-8
< Date: Sat, 02 Apr 2016 15:33:18 GMT
< Server: Apache-Coyote/1.1
< X-Application-Context: caffeine-service:cloud:0
< X-Vcap-Request-Id: a4e5e6dc-6c50-4a7d-6146-e5fa4039266d
<
$ curl -vGET -u d8b62608-ee1a-4978-a911-cf0c20a67b80:8fbe73dc-ea68-4ad4-b01b-6068ed3e22ec http://caffeine-service.local2.pcfdev.io/caffeine/e99e2910-6122-4dbf-a204-8297bb6e28a4/foo
> GET /caffeine/e99e2910-6122-4dbf-a204-8297bb6e28a4/foo HTTP/1.1
> Host: caffeine-service.local2.pcfdev.io
> Authorization: Basic ZDhiNjI2MDgtZWUxYS00OTc4LWE5MTEtY2YwYzIwYTY3YjgwOjhmYmU3M2RjLWVhNjgtNGFkNC1iMDFiLTYwNjhlZDNlMjJlYw==
> User-Agent: curl/7.43.0
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Length: 5
< Content-Type: text/plain;charset=UTF-8
< Date: Sat, 02 Apr 2016 15:33:25 GMT
< Server: Apache-Coyote/1.1
< X-Application-Context: caffeine-service:cloud:0
< X-Vcap-Request-Id: b7f95e95-ef44-41f5-43a1-1f6423a085a8
<
Hello
```