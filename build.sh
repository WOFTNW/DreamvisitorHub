cd ./dreamvisitor-web || exit
npm run build
cd ..
cp -r ./dreamvisitor-web/build/* ./src/main/resources/static/
mvn clean install