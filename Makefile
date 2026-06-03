# Find all Java files in the 'go' directory and its subdirectories
SOURCES = $(shell find go -name "*.java")

all: compile

compile:
	@echo "Compiling Java sources..."
	javac $(SOURCES)
	@echo "Compilation successful!"

test-channel: compile
	@echo "Running TestChannel..."
	java go.test.TestChannel

test-factory: compile
	@echo "Running TestFactory..."
	java go.test.TestFactory

test-shm01: compile
	@echo "Running TestShm01..."
	java go.test.TestShm01

test-shm03: compile
	@echo "Running TestShm03..."
	java go.test.TestShm03

test-shm11: compile
	@echo "Running TestShm11..."
	java go.test.TestShm11

test-shm13: compile
	@echo "Running TestShm13..."
	java go.test.TestShm13

test-shm20: compile
	@echo "Running TestShm20..."
	java go.test.TestShm20

test-all-shm: test-channel test-factory test-shm01 test-shm03 test-shm11 test-shm13 test-shm20
	@echo "All Shared Memory tests completed!"

test-cs01: compile
	@echo "Running TestCS01..."
	java go.test.TestCS01

test-cs03: compile
	@echo "Running TestCS03..."
	java go.test.TestCS03

test-cs11: compile
	@echo "Running TestCS11..."
	java go.test.TestCS11

test-cs13: compile
	@echo "Running TestCS13..."
	java go.test.TestCS13

test-all-cs: test-cs01 test-cs03 test-cs11 test-cs13
	@echo "All Client-Server tests "

clean:
	@echo "Cleaning up .class files..."
	find go -name "*.class" -type f -delete
	@echo "Clean complete"

.PHONY: all compile test-channel test-factory test-shm01 test-shm03 test-shm11 test-shm13 test-shm20 test-all-shm clean