This script is copied from https://github.com/Ra-Na/GTranslate-strings-xml 

First create a python3 virtual environment with:

`python3 -m venv .`

The activate it:

`source bin/activate`

Then install the requirements with: 

`pip install -r requirements.txt`

Copy the strings you want to translate to a local file called `strings.xml` and then run the script with:

PYTHONIOENCODING=utf8 python3 gtranslate.py strings.xml en fr de es

(The first language is the language we're translating from and the others are the languages we want to translate to)

Translated values should be found in the out directory
