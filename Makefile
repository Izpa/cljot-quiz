.PHONY: unit-test
unit-test:
	clojure -M:run-test:unit-test:common-test unit-test

.PHONY: integration-test
integration-test:
	clojure -M:run-test:integration-test:common-test integration

.PHONY: kibit
kibit:
	clojure -M:common-test:unit-test:integration-test:kibit --paths src,test

.PHONY: kondo
kondo:
	clojure -M:common-test:unit-test:integration-test:kondo --lint src test --paralell --cache false

.PHONY: eastwood
eastwood:
	clojure -M:common-test:unit-test:integration-test:eastwood

.PHONY: cljstyle-check
cljstyle-check:
	cljstyle check

.PHONY: cljstyle-fix
cljstyle-fix:
	cljstyle fix

.PHONY: build-uberjar
build-uberjar:
	clojure -T:build uber

.PHONY: run-uberjar
run-uberjar:
	java -jar target/cljot.jar

.PHONY: build-docker
build-docker:
	docker build . -t cljot

.PHONY: run-docker
run-docker:
	docker run -p ${SERVER_PORT}:${SERVER_PORT} --name cljot -d clot

.PHONY: run-dev
run-dev:
	clj -A:dev:common-test:unit-test:integration-test -X user/run-system!

.PHONY: run-system
run-system:
	docker-compose -f docker-compose.yml up -d

.PHONY: clj-deps
clj-deps:
	clj -X:deps prep 

.PHONY: all-checks
all-checks: cljstyle-check kibit kondo eastwood unit-test integration-test

