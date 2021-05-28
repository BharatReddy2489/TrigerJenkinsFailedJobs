package jenkins;

import static io.restassured.RestAssured.given;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;


public class Base extends Jenkins{
	
	public Properties prop;
	public static String user;
	public static String pwd;
	
	public String intialize(Properties prop) {
		user = prop.getProperty("user").trim();
		pwd = prop.getProperty("pwd").trim();
		RestAssured.baseURI = "http://ipjenkin02.ip.devcerner.net:8080/";
		String response = given().auth().preemptive().basic(user, pwd).when().get("crumbIssuer/api/json").then().extract().response().asString();
		JsonPath jsonData = new JsonPath(response); 
		String crumb = jsonData.getString("crumb");
		return crumb;
		
	}
	
	public Properties configProp() {
	
		prop=new Properties();
		try {
			
			FileInputStream fp = new FileInputStream("./resources/config/config.properties");
			prop.load(fp);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return prop;
	}
public static void main(String[] args) {
	System.out.println(user);
}
}
