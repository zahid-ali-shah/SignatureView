# SignatureView Release Process

1. 	Make sure it builds! and gradle sync does not produce any errors.

2. 	gradlew install
	
	Make sure there are no errors and warnings. Proceed to next step only if you see BUILD SUCCESSFULL message
	
2. 	gradlew clean build
	
	Check the version number in the root gradle.properties.

3. 	Make the release! and upload to bintray

	gradlew bintrayUpload
	
4.	Ensure availability on jcenter

	Tag commit as release (x.x.x)
	Provide changelog / .jar files at the Releases page.
	Increment the patch version number for future SNAPSHOT releases in the root gradle.properties.