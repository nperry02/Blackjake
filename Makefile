JAVAC = javac
JAVA = java
SRC = blackjack.java
CLASS = blackjack

.PHONY: all run clean

all: $(CLASS).class

$(CLASS).class: $(SRC)
	$(JAVAC) $(SRC)

run: all
	$(JAVA) $(CLASS)

clean:
	rm -f *.class
