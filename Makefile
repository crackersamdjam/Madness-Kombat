JAVAC ?= javac
JAVA ?= java
JAR ?= jar

SRC_DIR := src
RES_DIR := Resources
BIN_DIR := bin
JAR_FILE := MadnessKombat.jar
MAIN_CLASS := Main.GamePanel

SOURCES := $(shell find $(SRC_DIR) -name '*.java')

.PHONY: all compile resources jar run clean

all: compile resources

compile: $(SOURCES)
	mkdir -p $(BIN_DIR)
	$(JAVAC) --release 8 -encoding UTF-8 -d $(BIN_DIR) $(SOURCES)

resources:
	mkdir -p $(BIN_DIR)
	cp $(RES_DIR)/* $(BIN_DIR)/

jar: all
	$(JAR) --create --file $(JAR_FILE) --main-class $(MAIN_CLASS) -C $(BIN_DIR) .

run: all
	$(JAVA) -cp $(BIN_DIR) $(MAIN_CLASS)

clean:
	rm -rf $(BIN_DIR) $(JAR_FILE)
