#!/usr/bin/env python
# -*- coding: utf-8 -*-

#  Copyright 2014 Continuuity, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

#
# Checks that the license dependencies files used match the dependencies in the product.
# Run this script after building the SDK.
#


from optparse import OptionParser
import os
import subprocess
import sys
import tempfile

VERSION = "0.0.2"

DEFAULT_VERSION = "2.3.0-SNAPSHOT"

TEST_SINGLENODE = "singlenode"
SINGLENODE_RST = "reactor-singlenode-dependencies.rst"
LICENSE_MASTERS = "license_masters"
SINGLENODE_MASTER = "reactor-singlenode-dependencies.csv"

DEFAULT_TEST = TEST_SINGLENODE

REACTOR_VERSION_FILE = "../../../version.txt"

# DEFAULT_OUTPUT_PDF_FILE = "output.pdf"
# TEMP_FILE_SUFFIX = "_temp"

REST_EDITOR             = ".. reST Editor: "
RST2PDF                 = ".. rst2pdf: "
RST2PDF_BUILD           = ".. rst2pdf: build "
RST2PDF_CONFIG          = ".. rst2pdf: config "
RST2PDF_STYLESHEETS     = ".. rst2pdf: stylesheets "
RST2PDF_CUT_START       = ".. rst2pdf: CutStart"
RST2PDF_CUT_STOP        = ".. rst2pdf: CutStop"
RST2PDF_PAGE_BREAK      = ".. rst2pdf: PageBreak"
RST2PDF_PAGE_BREAK_TEXT = """.. raw:: pdf

	PageBreak"""
RST_WIDTHS = "   :widths:"
BACK_DASH = "\-"
DIVIDER = "======================================="

SCRIPT_DIR_PATH = os.path.dirname(os.path.abspath(__file__))

def set_version():
    # Sets the Reactor Build Version from the version.txt file
    ver_path = os.path.join(SCRIPT_DIR_PATH, REACTOR_VERSION_FILE)
    try:
        with open(ver_path,'r') as f:
            DEFAULT_VERSION = f.read()
            # print "DEFAULT_VERSION: %s" % DEFAULT_VERSION
    except:
        print "Couldn't read DEFAULT_VERSION from path: %s" % ver_path
        print "Using DEFAULT_VERSION: %s" % DEFAULT_VERSION


def parse_options():
    """ Parses args options.
    """

    set_version()

    parser = OptionParser(
        usage="%prog [options]",
        description="Checks that the license dependencies files used match the dependencies in the product.")

    parser.add_option(
        "-v", "--version",
        action="store_true",
        dest="version",
        help="Version of software",
        default=False)

    parser.add_option(
        "-b", "--buildversion",
        dest="build_version",
        help="The built version of the Continuuity SDK "
             "(default: %s)" % DEFAULT_VERSION,
        default=DEFAULT_VERSION)

    parser.add_option(
        "-t", "--test",
        dest="test",
        help="One of singlenode, other "
             "(default %s)" % DEFAULT_TEST,
        default=DEFAULT_TEST)

    parser.add_option(
        "-s", "--singlenode",
        action="store_true",
        dest="singlenode",
        help="Test singlenode",
        default=False)

    parser.add_option(
        "-r", "--read",
        action="store_true",
        dest="read",
        help="Reads in the master license files",
        default=False)

    (options, args) = parser.parse_args()

    if options.version:
        print "Version: %s" % VERSION
        sys.exit(1)

    if len(sys.argv) == 1:
#         print "sys.argv: %s" % len(sys.argv)
#         print "args: %s" % args
#         print "options: %s" % options
#         print "options.read: %s" % options.read
        parser.print_help()
        sys.exit(1)

#     return options, args[0]
    return options, args


def log(message, type):
    """Basic logger, print output directly to stdout and errors to stderr.
    """
    (sys.stdout if type == 'notice' else sys.stderr).write(message + "\n")
    
    
def process_masters():
    # Read in the master csv files and create some structures with them
    print "Reading master files"
    
    
def process_singlenode(input_file, options):
    # Read in the current rst file and create a structure with it
    # Read in the new singlenode csv file
    # Create and print to standard out the list of the references
    # Make a list of the references for which links are missing, that aren't in the master

    # Get the current singlenode dependencies master file
    #  "Package","Version","Classifier","License","License URL"
    csv_path = os.path.join(SCRIPT_DIR_PATH, LICENSE_MASTERS, SINGLENODE_MASTER)
    f = open(csv_path,'r')
    libs_dict = {}
    for line in f:
        l = line.strip('\n').strip()
        l = [subs.strip('"') for subs in l.split(',')]
        jar = l[0]
        try:
            lib = Library(l[0], l[3], l[4])
            print lib
            
            # Place lib reference in dictionary
            if not libs_dict.has_key(lib.id):
                libs_dict[lib.id] = lib
            else:
                print "Duplicate key: %s" %lib.id
                print "Current library: %s" % libs_dict[lib.id]
                print "New library: %s" % lib            
        except:
            print "   error with %s" % jar
            
            
    # Print out the results
    for lib in libs_dict.values():
        lib.pretty_print()
    
    
#     lib.set_max_sizes()
#     print "lib.MAX_SIZES: %s" % lib.MAX_SIZES
        
#         try:
#             lib = Library(l[0])
#             lib.package = l[1]
#             lib.license = l[3]
#             lib.license_url = l[4]
#             if lib.version != l[1]:
#                 try:
#                      if str(int(float(lib.version))) != l[1]:
#                         raise
#                 except:
#                     print "Version mis-match for %s\n  version: '%s', lib.version: '%s'" % (lib, l[1], lib.version)
#             if lib.classifier != l[2]:
#                 print "Classifier mis-match for %s\n  classifier: '%s', lib.classifier: '%s'" % (lib, l[2], lib.classifier)
#         except:
#             print "Lib %s" % l
#             raise
        
        # Place lib reference in dictionary
#         if not old_libs_dict.has_key(lib.id):
#             old_libs_dict[lib.id] = lib
#         else:
#             print "Duplicate key: %s" %lib.id
#             print "Current library: %s" % old_libs_dict[lib.id]
#             print "New library: %s" % lib

#     print "\nOld libs count: %s\n%s" % (len(old_libs), DIVIDER)
#     for l in old_libs:
#         print l

#     print "\nOld libs Dict count: %s\n%s" % (len(old_libs_dict), DIVIDER)
#     for l in old_libs_dict.keys():
#         print "%s: %s" % (l, old_libs_dict[l])



class Library:
    MAX_SIZES={}

    def __init__(self, jar, license, license_url):
        self.jar = jar # aka "package"
        self.id = ""
        self.base = ""
        self.version =  ""
        self.classifier = ""
        self.license = license
        self.license_url = license_url
        self.convert_jar()
        self.set_max_sizes()
        
    def __str__(self):
        return "%s : %s" % (self.id, self.jar)

    def convert_jar(self):
        import re
#         s = r'(?P<base>.*)-(?P<version>\d+[0-9.]*\d*)([.-]*(?P<classifier>.*?))\.jar$'
        s = r'(?P<base>.*?)-(?P<version>\d*[0-9.]*\d+)([.-]*(?P<classifier>.*?))\.jar$'
        try:
            m = re.match( s, self.jar)
            if m.group('classifier'):
                c = m.group('classifier')
            else:
                c = "<none>"
#             print "%s: %s %s %s" % (jar, m.group('base'), m.group('version'), c )
            self.base = m.group('base')
            self.version =  m.group('version')
            self.classifier = m.group('classifier')
            if self.classifier:
                self.id = "%s-%s" % (self.base, self.classifier)
            else:
                self.id = self.base
        except:
            raise

    def set_max_sizes(self):
        for element in self.__dict__.keys():
            if element[0] != "_":
                length = len(self.__dict__[element])
                if self.MAX_SIZES.has_key(element):
                    length = max(self.MAX_SIZES[element], length)
                self.MAX_SIZES[element] = length

    def pretty_print(self):
        SPACE = 2
        line = ""
        for element in self.__dict__.keys():
            if element[0] != "_":
                length = self.MAX_SIZES[element]
                line += self.__dict__[element].ljust(self.MAX_SIZES[element]+ SPACE)
        print line

def convert_package(jar):
    # Converts a package into a list of base, version, classifier
    INC_SNAPSHOT = "-incubating-SNAPSHOT.jar" # "twill-api-0.3.0-incubating-SNAPSHOT.jar"
    SNAPSHOT     = "-SNAPSHOT.jar" # app-fabric-2.3.0-SNAPSHOT.jar
    BETA_TEST    = "-beta-tests.jar" # "hadoop-common-2.1.0-beta-tests.jar"
    BETA         = "-beta.jar" # "hadoop-common-2.1.0-beta.jar"
    FINAL        = ".Final.jar" # async-http-servlet-3.0-3.0.8.Final.jar
    JAR          = ".jar" # "guice-servlet-3.0.jar"

    if text_ends_with(jar, INC_SNAPSHOT): # "twill-api-0.3.0-incubating-SNAPSHOT.jar"
        split = INC_SNAPSHOT
        classifier = "SNAP"
    elif text_ends_with(jar, SNAPSHOT): # "app-fabric-2.3.0-SNAPSHOT.jar"
        split = SNAPSHOT
        classifier = "SNAP"
    elif text_ends_with(jar, BETA_TEST): # "hadoop-common-2.1.0-beta-tests.jar"
        split = BETA_TEST
        classifier = "test"
    elif text_ends_with(jar, BETA): # "hadoop-common-2.1.0-beta.jar"
        split = BETA
        classifier = "beta"
    elif text_ends_with(jar, FINAL): # "async-http-servlet-3.0-3.0.8.Final.jar"
        split = FINAL
        classifier = "Final"
    elif text_ends_with(jar, JAR): # "guice-servlet-3.0.jar"
        split = JAR
        classifier = BACK_DASH
    else:
        raise Exception('convert_package', 'Unknown jar pattern: %s' % jar)
    
    base, version = jar_split(jar, split)
    if classifier == BACK_DASH:
        id = base
    else:
        id = "%s-%s" % (base, classifier)
    return id, [ base, version, classifier ]

def jar_split(jar, split):
    b = jar[:-len(split)] # "hadoop-common-2.1.0
    base = b[:b.rindex('-')] # "hadoop-common
    version = b[b.rindex('-')+1:] # 2.1.0
    return  base, version
   

# def process_pdf(input_file, options):
#     output = ""
#     config = ""
#     stylesheets = ""
#     print "input_file: %s" % input_file
#     f = open(input_file,'r')
#     lines = []
#     rst_copy = False
#     for line in f:
#         if line_starts_with(line, RST_WIDTHS):
#             rst_copy = True
#         elif not rst_copy or line.strip():
#             continue
#         else:
#             l = line.strip('\n')
#             l = l.strip()
#             l
#             lines.append(line.strip('\n'))
#             
#     # Set paths
#     source_path = os.path.dirname(os.path.abspath(__file__))
#     
#     # def get_absolute_path() Factor out duplicate code in this section
#     
#     if not os.path.isabs(input_file):
#         input_file = os.path.join(source_path, input_file)
#         if not os.path.isfile(input_file):
#             raise Exception('process_pdf', 'input_file not a valid path: %s' % input_file)
#             
#     if options.output_file:
#         output = options.output_file       
#     if not os.path.isabs(output):
#         output = os.path.join(os.path.dirname(input_file), output)
#         if not os.path.isdir(os.path.dirname(output)):
#             raise Exception('process_pdf', 'output not a valid path: %s' % output)
#             
#     if not os.path.isabs(config):
#         config = os.path.join(os.path.dirname(input_file), config)
#         if not os.path.isfile(config):
#             raise Exception('process_pdf', 'config not a valid path: %s' % config)
#             
#     if not os.path.isabs(stylesheets):
#         stylesheets = os.path.join(os.path.dirname(input_file), stylesheets)
#         if not os.path.isfile(stylesheets):
#             raise Exception('process_pdf', 'stylesheets not a valid path: %s' % stylesheets)
#             
#     # Write output to temp file
#     temp_file = input_file+TEMP_FILE_SUFFIX
#     if not os.path.isabs(temp_file):
#         raise Exception('process_pdf', 'temp_file not a valid path: %s' % temp_file)    
#     temp = open(temp_file,'w')    
#     for line in lines:
#         temp.write(line+'\n')
#     temp.close()
#     print "Completed parsing input file"
# 
#     # Generate PDF
# #     /usr/local/bin/rst2pdf 
# #     --config="/Users/john/Source/reactor_2.3.0_docs/docs/developer-guide/source/_templates/pdf-config" 
# #     --stylesheets="/Users/john/Source/reactor_2.3.0_docs/docs/developer-guide/source/_templates/pdf-stylesheet" 
# #     -o "/Users/john/Source/reactor_2.3.0_docs/docs/developer-guide/build-pdf/rest2.pdf" 
# #     "/Users/john/Source/reactor_2.3.0_docs/docs/developer-guide/source/rest.rst_temp”
#     command = '/usr/local/bin/rst2pdf --config="%s" --stylesheets="%s" -o "%s" %s' % (config, stylesheets, output, temp_file)
#     print "command: %s" % command
#     try:
#         output = subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT,)
#     except:
#         raise Exception('process_pdf', 'output: %s' % output)
#         
#     if len(output)==0:
#         os.remove(temp_file)
#     else:
#         print output
#         
#     print "Completed process_pdf"

#
# Utility functions
#
def quit():
    sys.exit(1)

def text_starts_with(text, left):
    return bool(text[:len(left)] == left)

def text_ends_with(text, right):
    return bool(text[-len(right):] == right)

def text_right_end(text, left):
    # Given a line of text (that may end with a carriage return) and a snip at the start,
    # return everything from the end of the snip onwards, except for the trailing return
    t = text[len(left):]
    return t.strip('\n')


#
# Main function
#

def main():
    """ Main program entry point.
    """
    options, input_file = parse_options()

    try:
        options.logger = log
#         if options.test == TEST_SINGLENODE:
#             process_singlenode(input_file, options)
        if options.singlenode:
            process_singlenode(input_file, options)
        elif options.read:
            process_masters()
#         elif options.test == "html":
#             print "HTML generation not implemented"
#         elif options.test == "slides":
#             print "Slides generation not implemented"
        else:
            print "Unknown test type: %s" % options.test
            sys.exit(1)
    except Exception, e:
        sys.stderr.write("Error: %s\n" % e)
        sys.exit(1)


if __name__ == '__main__':
    main()
