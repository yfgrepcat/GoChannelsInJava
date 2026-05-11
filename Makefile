# Find all Java files in the 'go' directory and its subdirectories
SOURCES = $(shell find go -name "*.java")

all: compile

compile:
	@echo "Compiling Java sources..."
	javac $(SOURCES)
	@echo "Compilation successful!"

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

test-all-shm: test-factory test-shm01 test-shm11 test-shm13 test-shm20
	@echo "All Shared Memory tests completed!"

clean:
	@echo "Cleaning up .class files..."
	find go -name "*.class" -type f -delete
	@echo "Clean complete!"

.PHONY: all compile test-factory test-shm01 test-shm03 test-shm11 test-shm13 test-shm20 test-all-shm clean