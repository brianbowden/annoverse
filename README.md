#Itsaverse

* Note: Building Tess-Two as an AAR and importing via Maven repo was a painful experience, and the Android Maven Plugin isn't quite ready for prime time when it comes to AAR generation. I had to hack in the "jni" native .so code directory into the AAR file after deploying to the local Maven repo, and to avoid doing that in the future, I just zipped it up straight from the repo as tess-two.zip. To rebuild all this, you'll need to first unzip tess-two.zip in your local repo (usually "~/.m2/repository/" if you're on OS X). Or, you could be adventurous and try to hack my pom.xml and setup.sh files into something more correct/useful in http://github.com/brianbowden/tess-two-bb.


