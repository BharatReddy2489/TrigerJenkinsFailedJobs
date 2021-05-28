package jenkins;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.restassured.path.json.JsonPath;

public class Jenkins {
	
	public static List<String> testScripts = new ArrayList<String>();
	public static List<String> jobNames = new ArrayList<String>();
	public static List<String> failedJobNames = new ArrayList<String>();
	public static List<String> ETFJobNames = new ArrayList<String>();
	public static List<String> ETFJobNamesFinal = new ArrayList<String>();
	public static String testResourcePath;
	public static String solutionName=System.getProperty("Solution");
	public static String domain=System.getProperty("DomainName");
	//public static String solutionName="Scheduling";
	//public static String domain="S18VA";	
	public static String testResult;
	public static int lastBuildID;
	public static String flag="";
	public static int waitTime=0;
	public static String lastBuildStatus="";
	public static String eteJobURL="job/Scheduling/job/E2E_RC_RCA_EP_S18VA_NODE1/";
	public static List<String> batchName = new ArrayList<String>();
	public static String token="E2E_RC_RCA_EP_S18VA_NODE1";
	public static String[] jenkinsDetails = new String[2];	
	
	public static List<String> getTOFJ(String user, String pwd, String crumb, String domain) {
		String ETFJobName;
		String ETFresponse = given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb)
				  .when().get("job/"+solutionName+"/api/json?pretty=true").then().extract().response().asString();
					JsonPath ETFJobNamesResponse = new JsonPath(ETFresponse);
					for(int i=0;i<ETFJobNamesResponse.getInt("jobs.name.size()");i++) {
						ETFJobName=ETFJobNamesResponse.get("jobs["+i+"].name");
						if(ETFJobName.contains("E2E")) {
							ETFJobNames.add(ETFJobName);
						}
						
					}
		return ETFJobNames;
	}
	
	public static void triggerETEJobsWithFailedJobs(String user, String pwd, String crumb, List<String> ETFJobNames) throws InterruptedException {
		List<String> failedJobNames = new ArrayList<String>();
		List<String> jobNamesLocal = new ArrayList<String>();
		String jsonJobNamesString="";
			for(int i=0;i<ETFJobNames.size();i++) {
				flag="";
				failedJobNames.clear();
				jobNamesLocal.clear();
				String batchres= given().auth().preemptive().basic(user, pwd).when().get("job/"+solutionName+"/job/"+ETFJobNames.get(i)+"/api/json?pretty=true").then().extract().response().asString();
				int index1=batchres.indexOf("ASSOCIATEDBATCH:");//returns the index of is substring  
				int index2=batchres.indexOf("(ENDOFASSOCIATEDBATCH)");//returns the index of index substring
				batchName.add((batchres.substring(index1, index2).replace("ASSOCIATEDBATCH:", "")).trim());
				String responseOfJobList=given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb)
					  .when().get("/job/"+solutionName+"/"+"view/Batches/job/"+batchName.get(i)+"/api/json?pretty=true").then().extract().response().asString();
			  JsonPath jsonJobNames = new JsonPath(responseOfJobList); 
			  for(int j=0;j<jsonJobNames.getInt("downstreamProjects.name.size()");j++) {
				  jsonJobNamesString = jsonJobNames.get("downstreamProjects["+j+"].name");
				  jobNamesLocal.add(jsonJobNamesString);
				}
					  String responseOfTestStatus;
					  for(int j=0;j<jobNamesLocal.size();j++) {
						  try {
						  responseOfTestStatus=given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb)
								  .when().get("/job/"+solutionName+"/job/"+jobNamesLocal.get(j)+"/lastBuild/api/json").then().extract().response().asString();
								  JsonPath jsonDataResult = new JsonPath(responseOfTestStatus);
								  testResult = jsonDataResult.getString("result");
								  if(!testResult.equals("SUCCESS")) {
									  flag="trigger";
									  failedJobNames.add(jobNamesLocal.get(j));
								  	}
						  }
								  catch(Exception e) {
									  }
				  
				  }
					  if (flag.equals("trigger")) {
						  ETFJobNamesFinal.add(ETFJobNames.get(i));
						  System.out.println("http://ipjenkin02.ip.devcerner.net:8080/job/"+solutionName+"/job/"+ETFJobNamesFinal.get(i));
						  given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb).queryParam("token", ETFJobNamesFinal.get(i))
								   .when().put("job/"+solutionName+"/job/"+ETFJobNamesFinal.get(i)+"/buildWithParameters").then().assertThat().statusCode(201);
								    System.out.println(ETFJobNamesFinal.get(i)+" is Triggered");
					  }
					  
		System.out.println("Total number of batch associated failed jobs: "+failedJobNames.size());
			}
			System.out.println("ETFJobNamesFinal: "+ETFJobNamesFinal);
	}
	
	public static void verifyETEJobStatus(String user, String pwd, String crumb){
		do {
			//get latest build status of E2E
			ETFJobNames.clear();
			for(int i=0;i<ETFJobNamesFinal.size();i++) {
				try {
			String responseOfLatestBuildStatus=given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb)
					  .when().get(eteJobURL+lastBuildID+"/api/json?pretty=true").then().extract().response().asString();
				JsonPath jsonDataResult = new JsonPath(responseOfLatestBuildStatus);
				lastBuildStatus = jsonDataResult.getString("result");
			
			  if(lastBuildStatus.equals("FAILURE")||lastBuildStatus.equals("SUCCESS")||lastBuildStatus.equals("ABORTED")) {
				  ETFJobNames.add(ETFJobNamesFinal.get(i));
				  Thread.sleep(30000); //300000 is 5 minutes
				  System.out.println(ETFJobNamesFinal.get(i)+ " is failed");
				  }
			  if(lastBuildStatus.equals("SUCCESS")) {
				  ETFJobNames.add(ETFJobNamesFinal.get(i));
				  Thread.sleep(30000); //300000 is 5 minutes
				  System.out.println(ETFJobNamesFinal.get(i)+ " is SUCCESS");
				  }
			  }
				catch(Exception e) {
					
					}
				}
			} while(!(ETFJobNamesFinal.size()==ETFJobNames.size()));
		System.out.println(ETFJobNames);
	}
	
	public static void triggerFailedJobs(String user, String pwd, String crumb, List<String> ETFJobNames) throws InterruptedException {
		List<String> jobNamesLocal = new ArrayList<String>();
		String jsonJobNamesString;
			for(int i=0;i<ETFJobNames.size();i++) {
				flag="";
				failedJobNames.clear();
				jobNamesLocal.clear();
				System.out.println(ETFJobNames.get(i));
				String batchres= given().auth().preemptive().basic(user, pwd).when().get("job/"+solutionName+"/job/"+ETFJobNames.get(i)+"/api/json?pretty=true").then().extract().response().asString();
				int index1=batchres.indexOf("ASSOCIATEDBATCH:");//returns the index of is substring  
				int index2=batchres.indexOf("(ENDOFASSOCIATEDBATCH)");//returns the index of index substring
				batchName.add((batchres.substring(index1, index2).replace("ASSOCIATEDBATCH:", "")).trim());
				System.out.println("batchURL: "+batchName.get(i));
				String responseOfJobList=given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb)
					  .when().get("/job/"+solutionName+"/"+"view/Batches/job/"+batchName.get(i)+"/api/json?pretty=true").then().extract().response().asString();
			  JsonPath jsonJobNames = new JsonPath(responseOfJobList); 
			  for(int j=0;j<jsonJobNames.getInt("downstreamProjects.name.size()");j++) {
				  jsonJobNamesString = jsonJobNames.get("downstreamProjects["+j+"].name"); 
				  jobNamesLocal.add(jsonJobNamesString);
				}
			  System.out.println("Total number associated jobs in specified resource path: "+jobNamesLocal.size());
					  String responseOfTestStatus;
					  for(int j=0;j<jobNamesLocal.size();j++) {
						  try {
						  responseOfTestStatus=given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb)
								  .when().get("/job/"+solutionName+"/job/"+jobNamesLocal.get(j)+"/lastBuild/api/json").then().extract().response().asString();
								  JsonPath jsonDataResult = new JsonPath(responseOfTestStatus);
								  testResult = jsonDataResult.getString("result");
								  if(!testResult.equals("SUCCESS")) {
									  given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb).queryParam("token", jobNames.get(i))
									   .when().put("job/"+solutionName+"/job/"+jobNamesLocal.get(j)+"/build").then().assertThat().statusCode(201);
									    System.out.println(jobNamesLocal.get(j)+" is Triggered");
								  	}
						  }
								  catch(Exception e) {
									  }
				  
				  }
					 
			}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		/*
		 * JTextField field1 = new JTextField(); JTextField field2 = new JTextField();
		 * Object[] message = { "Solution Name:", field1, "Domain:", field2, }; int
		 * option = JOptionPane.showConfirmDialog(null, message, "Enter the details",
		 * JOptionPane.OK_CANCEL_OPTION); if (option == JOptionPane.OK_OPTION) {
		 * jenkinsDetails[0] = field1.getText(); jenkinsDetails[1] = field2.getText(); } */		
		
		
		/*
		 * for(int i=0;i<ETFJobNames.size();i++) {
		 * System.out.println(ETFJobNames.get(i)); String batchres=
		 * given().auth().preemptive().basic(user,
		 * pwd).when().get("job/"+solutionName+"/job/"+ETFJobNames.get(i)+
		 * "/api/json?pretty=true").then().extract().response().asString(); int
		 * index1=batchres.indexOf("ASSOCIATEDBATCH:");//returns the index of is
		 * substring int index2=batchres.indexOf("(ENDOFASSOCIATEDBATCH)");//returns the
		 * index of index substring batchName.add((batchres.substring(index1,
		 * index2).replace("ASSOCIATEDBATCH:", "")).trim());
		 * System.out.println("batchURL: "+batchName.get(i));
		 * //jobListNames(crumb,solutionName,batchURL);
		 * verifyBatchAssociatedJobStatus(crumb, ETFJobNames, solutionName); if
		 * (flag.equals("Trigger")) { ETFJobNamesFinal.add(ETFJobNames.get(i));
		 * System.out.println(ETFJobNamesFinal.get(i)); }
		 * //verifyBatchJobStatus(crumb,jobNames,solutionName); }
		 */
		
		//get the failed job list in the particular batch
		
		
		/*
		 * if(flag.equals("Triggerk")) {
		 * 
		 * try { String
		 * responseOfTestStatus=given().auth().preemptive().basic(user,pwd).header(
		 * "Jenkins-Crumb",crumb)
		 * .when().get(eteJobURL+"api/json?pretty=true").then().extract().response().
		 * asString(); JsonPath jsonDataResult = new JsonPath(responseOfTestStatus);
		 * //testResult = jsonDataResult.getString("result"); lastBuildID =
		 * Integer.parseInt(jsonDataResult.getString("builds.number[0]"));
		 * }catch(Exception e) { System.out.println(e); } lastBuildID++;
		 * System.out.println("E2E is Triggered "+ lastBuildID);
		 * 
		 * given().auth().preemptive().basic(user,pwd).header("Jenkins-Crumb",crumb).
		 * queryParam("token", token)
		 * .when().put(eteJobURL+"buildWithParameters").then().assertThat().statusCode(
		 * 201); Thread.sleep(120000);
		 * 
		 * }
		 */	}

}
