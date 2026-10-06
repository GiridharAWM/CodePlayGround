# Use official Tomcat 10.1 with Eclipse Temurin JDK 21
FROM tomcat:10.1-jdk21-temurin

WORKDIR /usr/local/tomcat

# Remove default Tomcat web applications
RUN rm -rf webapps/*

# Copy WebContent into ROOT web application
COPY ["CodeVisualizationPlayground/WebContent/", "/usr/local/tomcat/webapps/ROOT/"]

# Copy Java source code (JSON array syntax handles spaces in folder name)
COPY ["CodeVisualizationPlayground/Java Resources/src/", "/usr/local/tomcat/src/"]

# Compile all Java source files into WEB-INF/classes
RUN mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/classes && \
    javac -cp "/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/*:/usr/local/tomcat/lib/*" \
    -d /usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
    $(find /usr/local/tomcat/src -name "*.java")

# Create symlink so both domain root "/" and "/CodeVisualizationPlayground" routes resolve
RUN ln -s /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/CodeVisualizationPlayground

# Expose default port
EXPOSE 8080

# Bind to Render's dynamic PORT if set, then launch Tomcat
CMD ["sh", "-c", "if [ -n \"$PORT\" ]; then sed -i \"s/port=\\\"8080\\\"/port=\\\"$PORT\\\"/g\" conf/server.xml; fi && catalina.sh run"]
