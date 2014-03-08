.PHONY: clean

all: | bin
	javac src/*.java -d bin

bin:
	mkdir $@

clean:
	rm -rf bin
