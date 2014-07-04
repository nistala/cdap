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

    # Get the current singlenode dependencies file
    #  "Package","Version","Classifier","License","License URL"
    rst_path = "../../developer-guide/source/licenses/%s" % SINGLENODE_RST
    rst_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), rst_path)
    f = open(rst_path,'r')
    old_libs = []
    old_libs_full = []
    old_libs_dict = {}
    rst_copy = False
    for line in f:
        if text_starts_with(line, RST_WIDTHS):
            rst_copy = True
        elif not rst_copy:
            continue
        elif line.strip() == '':
            continue
        elif rst_copy:
            l = line.strip('\n').strip()
            l = [subs.strip('"') for subs in l.split(',')]
            try:
                lib = Library(l[0])
            except:
                print "old_libs %s" % l
                raise
            if old_libs_dict.has_key(lib.id):
                print "Duplicate key: %s" %lib.id
                print "Current library: %s" % old_libs_dict[lib.id]
                print "New library: %ss" % lib
                
            else:
                old_libs_dict[lib.id] = lib

#             old_libs.append(lib)
#             old_libs_full.append(l) # "Package","Version","Classifier","License","License URL"
    
#     print "\nOld libs count: %s\n%s" % (len(old_libs), DIVIDER)
#     for l in old_libs:
#         print l

    print "\nOld libs Dict count: %s\n%s" % (len(old_libs_dict), DIVIDER)
    for l in old_libs_dict.keys():
        print "%s: %s" % (l, old_libs_dict[l])





#     print "\nOld libs complete\n====================="
#     for l in old_libs_full:
#         print l

def process_singlenode_old(input_file, options):
    print "%s generation..." % TEST_SINGLENODE
# Read the singlenode lib directory
#     
#     rel_lib_path = "../../../singlenode/target/sdk/continuuity-sdk-%s/lib" % options.build_version
#     lib_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), rel_lib_path)
#     if not os.path.isdir(lib_path):
#         raise Exception('process_singlenode', 'lib_path not a valid path: %s' % lib_path)
#     libs = os.listdir(lib_path)
#     new_libs = []
#     new_libs_full = []
#     for file in libs:
#         if file.startswith(".") or not text_ends_with(file, ".jar"):
#             continue
#         try:
#             lib = convert_package(file)
#         except:
#             print "new_libs %s" % file
#             raise
#         new_libs.append(lib[0]) # just the base
#         new_libs_full.append(lib) # base, version, classifier
#     
# Get the current singlenode dependencies file
#  "Package","Version","Classifier","License","License URL"
#     rst_path = "../../developer-guide/source/licenses/%s" % SINGLENODE_RST
#     rst_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), rst_path)
#     f = open(rst_path,'r')
#     old_libs = []
#     old_libs_full = []
#     old_libs_dict = {}
#     rst_copy = False
#     for line in f:
#         if text_starts_with(line, RST_WIDTHS):
#             rst_copy = True
#         elif not rst_copy:
#             continue
#         elif line.strip() == '':
#             continue
#         elif rst_copy:
#             l = line.strip('\n').strip()
#             l = [subs.strip('"') for subs in l.split(',')]
#             try:
#                 lib = convert_package(l[0])
#             except:
#                 print "old_libs %s" % l
#                 raise
# #             old_libs.append(lib[0]) # just the base
#             old_libs.append(lib[0]) # just the base
#             old_libs_full.append(l) # "Package","Version","Classifier","License","License URL"
    
    
#     print "\nNew libs... %d\n" % len(new_libs)
#     for lib in new_libs:
#         print lib
#     
#     brand_new = []
#     print "\nChecking new libs... %d\n" % len(new_libs)
#     for lib in new_libs:
#         if not lib in old_libs:
#             print "%s not in old libs" % lib
#             brand_new.append(lib)
#             
#     print "\n%d brand new libs" % len(brand_new)
    
    # Build new list
#     brand_new = []
#     for lib in new_libs:
#         i_n = new_libs.index(lib)
#         row = new_libs_full[i_n] #[0:2]
#         print row
#         if lib in old_libs:
#             i_o = old_libs.index(lib)
#             row += old_libs_full[i_o][3:4]             
#         brand_new.append(row)
#     for row in brand_new:
#         print row

            
#     print "\nOld libs... %d\n" % len(old_libs)
#     for lib in old_libs:
#         print lib
# 
#     print "\nChecking old libs... %d\n" % len(old_libs)
#     for lib in old_libs:
#         if not lib in new_libs:
#             print "%s not in new libs" % lib

    print "\nFinished process_singlenode"


class Library:

    def __init__(self, jar):
        self.jar = jar
        self.id = ""
        self.base = ""
        self.version =  ""
        self.classifier = ""
        self.url = ""
        self.convert_jar()

    def __str__( self ):
        return "%s:%s" % (self.id, self.jar)

    def convert_jar(self):
        # Converts a package into a list of base, version, classifier
        INC_SNAPSHOT = "-incubating-SNAPSHOT.jar" # "twill-api-0.3.0-incubating-SNAPSHOT.jar"
        SNAPSHOT     = "-SNAPSHOT.jar" # app-fabric-2.3.0-SNAPSHOT.jar
        BETA_TEST    = "-beta-tests.jar" # "hadoop-common-2.1.0-beta-tests.jar"
        BETA         = "-beta.jar" # "hadoop-common-2.1.0-beta.jar"
        FINAL        = ".Final.jar" # async-http-servlet-3.0-3.0.8.Final.jar
        JAR          = ".jar" # "guice-servlet-3.0.jar"
        DASH         = "-"
        
        if text_ends_with(self.jar, INC_SNAPSHOT): # "twill-api-0.3.0-incubating-SNAPSHOT.jar"
            split = INC_SNAPSHOT
            self.classifier = "SNAP"
        elif text_ends_with(self.jar, SNAPSHOT): # "app-fabric-2.3.0-SNAPSHOT.jar"
            split = SNAPSHOT
            self.classifier = "SNAP"
        elif text_ends_with(self.jar, BETA_TEST): # "hadoop-common-2.1.0-beta-tests.jar"
            split = BETA_TEST
            self.classifier = "test"
        elif text_ends_with(self.jar, BETA): # "hadoop-common-2.1.0-beta.jar"
            split = BETA
            self.classifier = "beta"
        elif text_ends_with(self.jar, FINAL): # "async-http-servlet-3.0-3.0.8.Final.jar"
            split = FINAL
            self.classifier = "Final"
        elif text_ends_with(self.jar, JAR): # "guice-servlet-3.0.jar"
            split = JAR
            self.classifier = DASH
        else:
            raise Exception('convert_jar', 'Unknown jar pattern: %s' % self.jar)
    
        self.base, self.version = jar_split(self.jar, split)
        if self.classifier == DASH:
            self.id = self.base
        else:
            self.id = "%s-%s" % (self.base, self.classifier)
    
    

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
