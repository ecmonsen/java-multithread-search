#!/bin/python
import subprocess
import nltk.tokenize
import re
import os

filename = "complete-works-of-shakespeare.txt"
if not os.path.exists(filename):
    subprocess.check_call(["wget", "https://www.gutenberg.org/ebooks/100.txt.utf-8", "-O", "complete-works-of-shakespeare.txt"])
with open(filename) as f:
	text = f.read()
uniq = set([t.lower() for t in nltk.tokenize.word_tokenize(re.sub("[_\[\]]", " ", text))])
with open("words.txt", "w") as f:
	f.write("\n".join(uniq))
	f.write("\n")
