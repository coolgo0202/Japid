import os, os.path
import sys
import shutil
import subprocess

try:
    from play.utils import package_as_war
    PLAY10 = False
except ImportError:
    PLAY10 = True

MODULE = 'japid'

COMMANDS = ['japid:gen', 'japid:regen', 'japid:mkdir', 'japid:clean']

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

#    print "japid command: " + command
    if command == 'japid:gen':
        run(app, 'gen')

    if command == 'japid:regen':
        run(app, 'regen')

    if command == 'japid:clean':
        run(app, 'clean')

    if command == 'japid:mkdir':
        run(app, 'mkdir')

def run(app, cmd):
    app.check()
    java_cmd = app.java_cmd(['-Xmx64m'], className='cn.bran.play.JapidCommands', args=[cmd])
#    print java_cmd                                                                                              
    subprocess.call(java_cmd, env=os.environ)
    print

#def after(**kargs):                                                                                             
#    command = kargs.get("command")                                                                              
#    app = kargs.get("app")                                                                                      
#    args = kargs.get("args")                                                                                    
#    env = kargs.get("env")                                                                                      
#    # what to do this? bran                                                                                     

def run10(cmd):
    check_application()
    do_classpath()
    do_java('cn.bran.play.JapidCommands')
    print "~ Ctrl+C to stop"
    java_cmd.append(cmd)
    subprocess.call(java_cmd, env=os.environ)
    print
    sys.exit(0)

if PLAY10:
    if play_command == 'japid:gen':
        run10('gen')

    if play_command == 'japid:regen':
        run10('regen')

    if play_command == 'japid:clean':
        run10('clean')

    if play_command == 'japid:mkdir':
        run10('mkdir')

