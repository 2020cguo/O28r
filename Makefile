CURR_DIR = $(PWD)

build-docker:
	docker build -t pmd .

run-docker:
	docker run -d --name pmd-container \
	--mount type=bind,source=.,target=/pmd \
	pmd tail -f /dev/null

login-docker:
	docker exec -it -e mvn=usr/share/maven/bin/mvn pmd-container bash

stop-docker:
	docker stop pmd-container
	docker rm pmd-container