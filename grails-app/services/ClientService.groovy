package com.securenow
import grails.transaction.Transactional

import java.text.DateFormat
import java.text.SimpleDateFormat

import com.securenow.client.Client
import com.securenow.external.Partner
import com.securenow.external.TPA
import com.securenow.pam.insurer.Insurer
import com.securenow.pam.insurer.ProspectInsurer
import com.securenow.security.ResetToken
import com.securenow.security.Role
import com.securenow.security.RoleManagement
import com.securenow.security.User
import com.securenow.security.UserRole
import com.securenow.trackers.JobTracker

@Transactional
class ClientService {
	def springSecurityService
	def commonService
	def repositoryService
	def sendMailService
	def fileUtilityService
	def utilService

	def client(params) {
		log.debug "dlt "+params.id
		Client currentClient =Client.findById(params.id)
		List<ClientProduct> clientProduct = ClientProduct.findAllByClient(currentClient)
		int noOfDisbledFields = 12 - (clientProduct != null && !clientProduct.isEmpty()?clientProduct.size:0)
		log.debug noOfDisbledFields+"  clientProduct " + clientProduct

		def role1 = Role.findByAuthority('ROLE_SERVICING_EXECUTIVE')
		def role2 = Role.findByAuthority('ROLE_SERVICING_MANAGER')
		def role3 = Role.findByAuthority('ROLE_RELATIONSHIP_MANAGER')
		def role4 = Role.findByAuthority('ROLE_BUSINESS_HEAD')

		def se1 = [:]
		def se2 = [:]
		def rm1 = [:]
		def rm2 = [:]
		def bh  = [:]

		def clientUserse = ClientUser.findAllByClient(currentClient)

		if(clientUserse){
			clientUserse.each {
				UserRole userRole=UserRole.findByUser(it.user)
				if(userRole.role.authority.equals("ROLE_SERVICING_EXECUTIVE") && se1.size() ==0 ){
					se1.id=it.user.id
					se1.name=it.user.name
					se1.email=it.user.email
					se1.phone=it.user.username
					se1.role=userRole.role.authority
				}
				else if(userRole.role.authority.equals("ROLE_SERVICING_MANAGER") && sm1.size() ==0 ){
					sm1.id=it.user.id
					sm1.name=it.user.name
					sm1.email=it.user.email
					sm1.phone=it.user.username
					sm1.role=userRole.role.authority
				}
				else if(userRole.role.authority.equals("ROLE_RELATIONSHIP_MANAGER") && rm1.size() ==0 ){
					rm1.id=it.user.id
					rm1.name=it.user.name
					rm1.email=it.user.email
					rm1.phone=it.user.username
					rm1.role=userRole.role.authority
				}
				else if(userRole.role.authority.equals("ROLE_RELATIONSHIP_MANAGER") && rm1.size() !=0 && rm2.size() ==0){
					rm2.id=it.user.id
					rm2.name=it.user.name
					rm2.email=it.user.email
					rm2.phone=it.user.username
					rm2.role=userRole.role.authority
				}

				if(userRole.role.authority.equals("ROLE_BUSINESS_HEAD") && bh.size() ==0){
					bh.id=it.user.id
					bh.name=it.user.name
					bh.email=it.user.email
					bh.phone=it.user.username
					bh.role=userRole.role.authority
				}
			}
		}
		def userrole=UserRole.findByUser(springSecurityService.currentUser)

		def seList
		if(role1)
			seList=UserRole.findAllByRole(role1)

		def smList
		if(role2)
			smList=UserRole.findAllByRole(role2,[sort: "id", order: "desc"])

		def rmList
		if(role3)
			rmList=UserRole.findAllByRole(role3)


		def bhList
		if(role4)
			bhList=UserRole.findAllByRole(role4)
		List<Product> listProduct=Product.list()
		[dlt:params.dlt,se1:se1,sm1:sm1, se2:se2,rm1:rm1,rm2:rm2,bh:bh,smList : smList, bhList:bhList,rmList:rmList,seList:seList,clientProduct : clientProduct, noOfDisbledFields : noOfDisbledFields, currentClient :currentClient,productlist:listProduct,userrole:userrole]
	}

	def clientProductDetail(params,userrole){
		def latest
		def currentClient=Client.findById(params.clientId)
		def product=Product.findById(params.id)
		ClientProduct currentClientProduct = ClientProduct.findByClientAndProduct(currentClient, product)
		log.debug "currentClientProduct " + currentClientProduct
		def insurers=Insurer.list()
		def role3 = Role.findByAuthority('ROLE_RELATIONSHIP_MANAGER')
		def rmList
		if(role3)
			rmList=UserRole.findAllByRole(role3)
		def ROLE_TPA
		if(product && currentClient){
			ROLE_TPA=TPA.list()

			def clientProduct=ClientProduct.findAllByClientAndProduct(currentClient,product)
			def clientProductdetails
			def endorsementlist
			def endorsementlistNotApproved
			def policydetails=[]
			DateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy")
			if(clientProduct){
				clientProductdetails=ClientProductDetail.findAllByClientProduct(clientProduct,[sort: "id", order: "desc"])
				if(clientProductdetails){
					if(clientProductdetails.last().insurer?.id)
						latest = clientProductdetails.last().insurer?.id
					else
						latest='No'
					clientProductdetails.each {
						def eachDetails=[:]
						eachDetails.id=it.id
						eachDetails.start_date=dateFormat.format(it.start_date)
						eachDetails.end_date=dateFormat.format(it.end_date)
						eachDetails.policy_type=it.policy_type
						eachDetails.policyHolder = it.policyHolder
						eachDetails.policy_number=it.policy_number
						eachDetails.premium=it.premium
						eachDetails.sum_assured=it.sum_assured
						eachDetails.tPA=it?.tPA?.name
						if((it?.insurer?.name).length()>30){
							eachDetails.insurer=(it?.insurer?.name).substring(0,27)+"..."
						}else{
							eachDetails.insurer=it?.insurer?.name
						}
						eachDetails.claimsToDate=it.claimsToDate
						eachDetails.isDispatched=it.isDispatched
						policydetails.add(eachDetails)
					}
				}else{
					latest='No'
				}
				endorsementlist=Endorsement.findAllByClientProduct(clientProduct,[sort: "id", order: "desc"])
				endorsementlistNotApproved=Endorsement.findAllByClientProductAndIsAccepted(clientProduct,false)
				log.debug "policydetails " + policydetails
			}
			def allPolicies = Dropdown.findAllByType('Policy Type-Entry', [sort:'name'])
			def policyServices = Dropdown.findAllByType('Policy Type-Service', [sort:'name'])
			[allPolicies : allPolicies, latest:latest,endorsementlistNotApproved:endorsementlistNotApproved,
				endorsementlist:endorsementlist,userrole:userrole,insurers:insurers,currentClient:currentClient,
				partner:Partner.list(),product:product,rmList:rmList,id:params.id,clientId:params.clientId,
				clientProductdetails:policydetails,ROLE_TPA:ROLE_TPA,policyServices:policyServices]
		}
	}


	def changeContactUserForClient(params){
		log.debug "changeContactUserForClient "+params
		HashMap res = new HashMap()
		User user = User.findById(params.id)
		Client client = Client.findById(params.cid)
		client.date = new Date()
		client.save()
		User oluser = User.findById(params.oid)
		if(oluser){
			def cliuser=ClientUser.findByClientAndUser(client,user)
			log.debug "cliuser  "+cliuser
			if(cliuser){
				def urole=UserRole.findByUser(oluser)
				res.id=oluser.id
				res.name=oluser.name
				res.email=oluser.email
				res.mobile=oluser.username
				res.role=urole.role.authority
				res.message="Already Assigned"
				return res
			}else{
				def newcliuser=ClientUser.findByClientAndUser(client,oluser)
				log.debug "client " + client +" oluser " + oluser
				log.debug "newcliuser " + newcliuser
				newcliuser.user=user
				def urole=UserRole.findByUser(user)
				res.id=user.id
				res.name=user.name
				res.email=user.email
				res.mobile=user.username
				res.role=urole.role.authority
				res.message="Successfully Assigned"
				newcliuser.save(flush:true)
				log.debug"newcliuser. " + newcliuser
				return res
			}
		}else{
			log.debug "new  "
			def cliuser=ClientUser.findByClientAndUser(client,user)
			if(cliuser){
				res.message="Already Assigned"
				return res
			}else{
				def newCliuserenter=new ClientUser()
				newCliuserenter.user=user
				newCliuserenter.client=client
				def urole=UserRole.findByUser(user)
				res.id=user.id
				res.name=user.name
				res.email=user.email
				res.mobile=user.username
				res.role=urole.role.authority
				res.message="Successfully Assigned"
				newCliuserenter.save(flush:true)
				assignToSupervisor(client, urole.role.authority)
				return res
			}
		}
	}

	/**
	 * Policy Renewal Job
	 * Fetches all the Client Product Details, where renewal date within the next 90 days, and isMovedToUnassigned=false, 
	 * then perform the below operations:
	 * 1. Move to action required of RM
	 * 2. if policy renewal due in 23 days and 1 day, send email to RM and client(if client policy alerts true) 
	 * @return
	 */
	def policyNotification(){
		boolean mailSent = sendMailService.jobMail("start" , "Policy Renewal Job")
		log.debug("mailSent for policy job "+mailSent)
		JobTracker jobTracker = utilService.setTracker("Policy Renewal")
		//Get all clients 
		List clientList = Client.findAllByIsClient(true)
		SimpleDateFormat formater = new SimpleDateFormat("dd-MM-yyyy")
		String day90 = getRenewalDateStr(90)
		String day23 = getRenewalDateStr(23)
		String day1 = getRenewalDateStr(1)
		
		//Adding client in to list or cc list for mail if role client then in to otherwise in cc
		clientList.each { client->
			List clientUsers = ClientUser.findAllByClient(client)
			List toEmail = []
			List ccEmail = []
			clientUsers.each { clientUser->
				String userEmail = clientUser?.user?.email
				if(userEmail) {
					UserRole userRole = UserRole.findByUser(clientUser?.user)
					if(userRole?.role?.authority?.equals('ROLE_CLIENT') && clientUser?.alert?.equals("Yes")) {
						toEmail.add(userEmail)
					}else if(!userRole?.role?.authority?.equals('ROLE_CLIENT')) {
						ccEmail.add(userEmail)
					}
				}
			}
			// All client products
			List clientProducts = ClientProduct.findAllByClient(client)
			
			try {
				String renewalDate
				//TO check for every client product
				clientProducts.each { clientProduct->
					clientProduct?.clientProductDetail.each{ 
						ClientProductDetail clientProductDetail =  (ClientProductDetail)ClientProductDetail.get(it.id)
						if(clientProductDetail?.policy_type.equals("Base")){
							renewalDate = formater.format(clientProductDetail?.end_date)
							//log.debug("day90 "+day90.substring(6, 10)+"   "+renewalDate.substring(6, 10))
							//if(day30?.equals(renewalDate) || day23?.equals(renewalDate) || day15?.equals(renewalDate) || day7?.equals(renewalDate) || day3?.equals(renewalDate)) {
							/**
							 * if renewal date is after 23 days or 1 day, AND the client policy alert is YES, send email.
							 */
							if( (day23?.equals(renewalDate) || day1?.equals(renewalDate)) && clientProductDetail?.alert?.equals("Yes")) {
								Map value = [company	: client?.company,
									product	: clientProduct?.product?.product_name,
									renewalDate: renewalDate,
									sales		: clientProductDetail?.user?.name,
									to			: toEmail,
									cc			: ccEmail
								]
								boolean isMailSent = sendMailService.policyRenewalReminder(value)
								log.debug("isMailSent of renewal with client "+isMailSent)
								
							}
							else if(day23?.equals(renewalDate) || day1?.equals(renewalDate)){
								Map value = [company	: client?.company,
									product	: clientProduct?.product?.product_name,
									renewalDate: renewalDate,
									sales		: clientProductDetail?.user?.name,
									to			: ccEmail,
									cc			: []
								]
								boolean isMailSent = sendMailService.policyRenewalReminder(value)
								log.debug("isMailSent of renewal to RM "+isMailSent)
							}
							DateFormat form = new SimpleDateFormat("dd-MM-yyyy")
							Date date90 = form.parse(day90)
							Date renewal = form.parse(renewalDate)
							log.debug("date "+date90+"  "+clientProductDetail?.isMovedToUnassigned)
							if(date90 > renewal && !clientProductDetail?.isMovedToUnassigned){
								log.debug("--move to prospect--"+clientProductDetail.isMovedToUnassigned)
								createProspectForRenewal(clientProductDetail)
								clientProductDetail.isMovedToUnassigned = true
								clientProductDetail.save()
								log.debug("--moved to prospect--")
							}
						}
					}
				}
			}catch(Exception ex){
				log.debug("exception in policyNotification "+client)
				log.debug(ex.getMessage())
				mailSent = sendMailService.policyJobExceptionMail(ex.getMessage() , client)
				log.debug("mailSent "+mailSent) 
				//System.exit
			}
		}
		mailSent = sendMailService.jobMail("complete" , "Policy Renewal")
		log.debug("mailSent for policy job complete "+mailSent)
		utilService.endTracker(jobTracker)
	}

	def createProspectForRenewal(ClientProductDetail policy){
		Client client = policy?.clientProduct?.client
		def user = ClientUser.findByClient(policy?.clientProduct?.client).user
		def product = policy?.clientProduct?.product
		Client prospect = new Client()
		
		//Save client product detail details of prospect to track the prospect policy details
		ClientProductDetail clientProdDetail = new ClientProductDetail()
		clientProdDetail.user = policy?.user
		clientProdDetail.end_date = policy?.end_date
		clientProdDetail.start_date = policy?.start_date
		clientProdDetail.premium = policy?.premium
		clientProdDetail.policyHolder = policy?.policyHolder
		clientProdDetail.policy_number = policy?.policy_number
		
		prospect.company = client?.company
		prospect.location = client?.location
		prospect.channel = policy?.channel ? policy?.channel : client.channel 
		//---------------------------issue here of comment name------
		//prospect.comments = "Product renewal"
		prospect.comment = "Product renewal"
		prospect.partner = policy?.partner
		prospect.status = "Unassigned"
		prospect.name = (client?.name == null)?user?.name:client.name
		prospect.mobile = (client?.mobile == null)?user?.username:client.mobile
		prospect.email = (client?.email == null)?user?.email:client.email
		prospect.designation = (client?.designation == null)?user?.designation:client.designation
		prospect.isClient = false
		//prospect.save(flush:true)
		prospect.save()
		/*boolean isProductSaved = new ClientProduct(client: client, product: product).save()
		 log.debug("isProductSaved "+isProductSaved)*/
		ClientProduct cp = new ClientProduct(client: prospect, product: product).save()
		
		clientProdDetail.clientProduct = cp
		clientProdDetail.save()
		List<ClientProductDetail> lcpd = new ArrayList<ClientProductDetail>()
		lcpd.add(clientProdDetail)
		
		cp.clientProductDetail = lcpd
		cp.save()
	}


	def deleteClient(def params,def attachmentLocation) {
		Map res = new HashMap()
		List roleManagement
		if(params.id){
			List cpId = []
			Client client = Client.findById(params.id)
			log.debug "Delete client = "+client?.id
			if(client){
				boolean isBackUpMailSent = sendMailService.sendClientBackUpDetails(client)
				log.debug("isBackUpMailSent "+isBackUpMailSent)
				if(client.isClient){
					def clientUser = ClientUser.findAllByClient(client)
					log.debug("clientUser "+clientUser)
					List user = new ArrayList()
					clientUser.each{
						if(UserRole.findByUser(it?.user)?.role?.authority.equals("ROLE_CLIENT")){
							user.add(it.user)
						}
						log.debug(user)
					}
					roleManagement = RoleManagement.findAllByUserInList(user)
					log.debug("rm "+roleManagement)
					if(roleManagement)
						roleManagement*.delete(flush:true)
				}
				
				//Delete All reminders
				List reminder = Reminder.findAllByClient(client)
				if(reminder)
					reminder*.delete(flush:true)
				
				
				List clietUserList = ClientUser.findAllByClient(client)
				clietUserList.each {
					UserRole userRole = UserRole.findByUser(it.user)
					if(userRole?.role?.authority?.equals("ROLE_CLIENT")) {
						List userRoleList = UserRole.findAllByUser(it.user)
						List tokenList = ResetToken.findAllByUser(it.user)
						tokenList.each(){
							it.delete(flush:true)
						}
						userRoleList.each(){
							if(it.role?.authority?.equals("ROLE_CLIENT")) {
								cpId.add(it.user?.id)
							}
							it.delete(flush:true)
						}
					}
					it.delete(flush:true)
				}
				List historyList = ClientHistory.findAllByClient(client)
				historyList*.delete(flush:true)
				List prospectInsurerList = ProspectInsurer.findAllByClient(client)
				prospectInsurerList*.delete(flush:true)
				List clientProductList = ClientProduct.findAllByClient(client)
				clientProductList.each {
					List policyList = ClientProductDetail.findAllByClientProduct(it)
					List endorsementList = Endorsement.findAllByClientProduct(it)
					policyList*.delete(flush:true)
					endorsementList*.delete(flush:true)
					it.delete(flush:true)
				}
				commonService.deleteDirectory(new File(attachmentLocation + File.separator + "endorsements" + File.separator + "client_" +client?.id))
				commonService.deleteDirectory(new File(attachmentLocation + File.separator + "policies" + File.separator + "client_" +client?.id))
				if(client?.logo) {
					fileUtilityService.deleteFile(attachmentLocation + File.separator + "clientsLogo" + File.separator + client?.logo)
				}
				cpId.each {
					User user = User.findById(it)
					List policy = ClientProductDetail.findAllByContactperson(user)
					policy.each {
						log.debug "CP found in policy"
						it.contactperson = null
						it.save(flush:true)
					}
					List clientUser = ClientUser.findAllByUser(user)
					clientUser.each {
						it.delete(flush:true)
					}
					user.delete()
				}
				
				client.delete(flush:true)
				def isDeleted = Client.findById(params.id)
				if(!isDeleted){
					log.debug "client deleted successfully "
					res.result = "success"
					res.message = "Client deleted sucessfully"
					return res
				}else{
					res.result = "failed"
					res.message = "Error while deleting client"
				}
			}else{
				res.result = "failed"
				res.message = "Invalid client id"
			}
		}else{
			res.result = "failed"
			res.message = "Client id required"
		}
		return res
	}

	private String getRenewalDateStr(int before){
		SimpleDateFormat formater = new SimpleDateFormat("dd-MM-yyyy")
		Calendar cal = Calendar.getInstance()
		cal.setTime(new Date())
		cal.add(Calendar.DATE, before)
		return formater.format(cal.getTime())
	}

	def assignToSupervisor(Client client, String contactManagerRole){
		int maxContactManager = 2
		if(contactManagerRole?.equals("ROLE_RELATIONSHIP_MANAGER")){
			maxContactManager = 3
		}
		Role role = Role.findByAuthority(contactManagerRole)
		List userList = UserRole.findAllByRole(role)*.user
		List contactManagerList = ClientUser.findAllByClientAndUserInList(client, userList)
		if(contactManagerList.size()>0 && contactManagerList.size()<maxContactManager){
			User supervisor = contactManagerList.get(0).user.supervisor
			if(supervisor){
				if(!contactManagerList*.id.contains(supervisor?.id)){
					ClientUser contactManager2 = new ClientUser(client : client, user : supervisor)
					contactManager2.save()
				}
				if( contactManagerList.size() != (maxContactManager-1) && supervisor.supervisor){
					if(!contactManagerList*.id.contains(supervisor?.supervisor?.id)){
						ClientUser contactManager3 = new ClientUser(client : client, user : supervisor.supervisor)
						contactManager3.save()
					}
				}
			}
		}
	}

	def contactMeMail(Client client){
		List contactManagers = getContactManager(client)
		log.debug("contactManagers "+contactManagers*.email)
		return contactManagers*.email
	}

	def getContactManager(Client client){
		List clientUserList = ClientUser.findAllByClient(client)*.user
		Role clientRole = Role.findByAuthority("ROLE_CLIENT")
		List contactManagers = UserRole.findAllByUserInListAndRoleNotEqual(clientUserList, clientRole)*.user
		return contactManagers
	}

	def getPolicyStatistics(User rm){
		Map res = new HashMap()
		Calendar cal = Calendar.getInstance()
		//building last year's current date
		def dateBuilder = new GregorianCalendar(cal.get(Calendar.YEAR)-1, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
		def lastYearCurrentDate = dateBuilder.getTime()
		log.debug("lastYearCurrentDate "+lastYearCurrentDate)
		
		dateBuilder.set(Calendar.DAY_OF_MONTH, 1)
		def lastYearCurrentMonth = dateBuilder.getTime()
		log.debug("lastYearCurrentMonth "+lastYearCurrentMonth.getClass())
		log.debug("month "+cal.get(Calendar.MONTH));
//		cal.setTime(lastYearCurrentMonth);
		
		log.debug "cal.get(Calendar.YEAR) " + cal.get(Calendar.YEAR)
		
		def lastYearFirstApril
		def currentYearFirstApril
		//for Jan, Feb. March, the current April should be (current year - 1), for April to December, the current April 
		//would be current year
		if(cal.get(Calendar.MONTH) < 4){  
			log.debug "inside if "
			dateBuilder.set(cal.get(Calendar.YEAR)-2, Calendar.APRIL, 1)
			lastYearFirstApril = dateBuilder.getTime()
			log.debug "lastYearFirstApril " + lastYearFirstApril
			
			//setting current year's first april
			dateBuilder.set(cal.get(Calendar.YEAR)-1, Calendar.APRIL, 1)
			currentYearFirstApril = dateBuilder.getTime()
		}
		else{
			log.debug "inside else"
			dateBuilder.set(cal.get(Calendar.YEAR) - 1, Calendar.APRIL, 1)
			lastYearFirstApril = dateBuilder.getTime()
			log.debug "lastYearFirstApril " + lastYearFirstApril
			
			//setting current year's first april
			dateBuilder.set(cal.get(Calendar.YEAR), Calendar.APRIL, 1)
			currentYearFirstApril = dateBuilder.getTime()
		}
		  
		//setting current year
		dateBuilder.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
		def today = dateBuilder.getTime()
		
		//setting current month
		dateBuilder.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1)
		def currentMonth = dateBuilder.getTime()
		
		log.debug "currentYearFirstApril " + currentYearFirstApril
		log.debug "today " + today
		def LYTDList1
		def LYTDList, CYTDList, LMTDList, CMTDList
		log.debug("lastYearFirstApril "+lastYearFirstApril) 
		log.debug("lastYearCurrentDate "+lastYearCurrentDate)
		if(rm){
			//for RM
			LYTDList = ClientProductDetail.findAllByUserAndStart_dateBetween(rm, lastYearFirstApril, lastYearCurrentDate)
			CYTDList = ClientProductDetail.findAllByUserAndStart_dateBetween(rm, currentYearFirstApril, today)
			LMTDList = ClientProductDetail.findAllByUserAndStart_dateBetween(rm, lastYearCurrentMonth, lastYearCurrentDate)
			CMTDList = ClientProductDetail.findAllByUserAndStart_dateBetween(rm, currentMonth, today)
		}else{
			//for Admin
			log.debug "admin login"
			//LYTDList = ClientProductDetail.findAllByStart_dateBetween(lastYearFirstApril, lastYearCurrentDate)
			//CYTDList = ClientProductDetail.findAllByStart_dateBetween(currentYearFirstApril, today)
			CYTDList = ClientProductDetail.createCriteria().list{
				between("start_date", currentYearFirstApril, today)
				projections {
					property('premium')
					property('commission')
					clientProduct {
						
						 client{
							 property('id')
						   }

						 }
				}
			}
			
			//LMTDList = ClientProductDetail.findAllByStart_dateBetween(lastYearCurrentMonth, lastYearCurrentDate)
			
			LMTDList = ClientProductDetail.createCriteria().list{
				between("start_date", lastYearCurrentMonth, lastYearCurrentDate)
				projections {
					property('premium')
					property('commission')
					clientProduct {
						
						 client{
							 property('id')
						   }

						 }
				}
			}
			//CMTDList = ClientProductDetail.findAllByStart_dateBetween(currentMonth, today)
			
			CMTDList = ClientProductDetail.createCriteria().list{
				between("start_date", currentMonth, today)
				projections {
					property('premium')
					property('commission')
					clientProduct {
						
						 client{
							 property('id')
						   }

						 }
				}
			}
			
			
		   LYTDList = ClientProductDetail.createCriteria().list{
				between("start_date", lastYearFirstApril, lastYearCurrentDate)
				projections {
					property('premium')
					property('commission')
					clientProduct {
					    
						 client{
							 property('id')
						   }

						 }
				}
			}

		}
		def clients = [:]
		def premium = [:]
		def commission = [:]

		def premiumLYTD = LYTDList.sum{
			if(it[0]!=null && it[0]!="")
				Double.parseDouble(it[0])
			else
				0.0
		}
		log.debug("premiumLYTD "+premiumLYTD)
		def premiumCYTD = CYTDList.sum{
			if(it[0]!=null && it[0]!="")
				Double.parseDouble(it[0])
			else
				0.0
		}
		log.debug("premiumCYTD "+premiumCYTD)
		//--------------issue here LYTDList should be replaced with LMTDList---------
		def premiumLMTD = LMTDList.sum{
			if(it[0]!=null && it[0]!="")
				Double.parseDouble(it[0])
			else
				0.0
		}
		def premiumCMTD = CMTDList.sum{
			if(it[0]!=null && it[0]!="")
				Double.parseDouble(it[0])
			else
				0.0
		}
		
		def commissionLYTD = LYTDList?.sum{
			log.debug("it?.premium "+it[0])
			log.debug("it?.commission "+it[1])
			if(it[0]!=null && it[0]!=""){
				Double.parseDouble(it[0])* it[1]/100
			}
			else
				0.0
		}
		log.debug("commissionLYTD "+commissionLYTD)
		
		def commissionCYTD = CYTDList?.sum{
			if(it[0]!=null && it[0]!=""){
				Double.parseDouble(it[0])* it[1]/100
			}
			else
				0.0
		}
		log.debug("commissionCYTD "+commissionCYTD)
		//------------same issue LYTDList should replaced with LMTDList
		def commissionLMTD = LMTDList.sum{
			if(it[0]!=null && it[0]!="")
				Double.parseDouble(it[0])* it[1]/100
			else
				0.0
		}
		def commissionCMTD = CMTDList.sum{
			if(it[0]!=null && it[0]!="")
				Double.parseDouble(it[0])* it[0]/100
			else
				0.0
		}
		premium.LYTD = Math.round(premiumLYTD ? premiumLYTD : 0.0)
		premium.CYTD = Math.round(premiumCYTD ? premiumCYTD : 0.0)
		premium.LMTD = Math.round(premiumLMTD ? premiumLMTD : 0.0)
		premium.CMTD = Math.round(premiumCMTD ? premiumCMTD : 0.0)
		
		commission.LYTD = Math.round(commissionLYTD ? commissionLYTD : 0.0)
		commission.CYTD = Math.round(commissionCYTD ? commissionCYTD : 0.0)
		commission.LMTD = Math.round(commissionLMTD ? commissionLMTD : 0.0)
		commission.CMTD = Math.round(commissionCMTD ? commissionCMTD : 0.0)
		log.debug("lastYearCurrentMonth "+lastYearCurrentMonth)
		log.debug("lastYearCurrentDate "+lastYearCurrentDate)
		// clients since inception
		if(!rm){
			LYTDList = ClientProductDetail.findAllByStart_dateLessThanEquals(lastYearCurrentDate)
			CYTDList = ClientProductDetail.findAllByStart_dateLessThanEquals(today)
			LMTDList = ClientProductDetail.findAllByStart_dateBetween(lastYearCurrentMonth, lastYearCurrentDate)
			CMTDList = ClientProductDetail.findAllByStart_dateBetween(currentMonth, today)
		}
		
		clients.LYTD = LYTDList*.clientProduct*.client.unique { a, b -> a.id <=> b.id }.size()
		 clients.CYTD = CYTDList*.clientProduct*.client.unique { a, b -> a.id <=> b.id }.size() + clients.LYTD
		 //--------Issue in client list--------------
		 //clients.LMTD = LYTDList*.clientProduct*.client.unique { a, b -> a.id <=> b.id }.size()
		 clients.LMTD = LMTDList*.clientProduct*.client.unique { a, b -> a.id <=> b.id }.size()
		 clients.CMTD = CMTDList*.clientProduct*.client.unique { a, b -> a.id <=> b.id }.size() 

		//------- as per new formula--
		//clients.LYTD = LYTDList*.clientProduct*.client.unique { a, b -> a?.id <=> b?.id }.size()
		clients.CYTD = CYTDList*.clientProduct*.client.unique { a, b -> a?.id <=> b?.id }.size() + clients?.LYTD
		clients.LMTD = LMTDList*.clientProduct*.client.unique { a, b -> a?.id <=> b?.id }.size()
		clients.CMTD = CMTDList*.clientProduct*.client.unique { a, b -> a?.id <=> b?.id }.size()
		//------- updated--
		log.debug("premium "+premium)
		Set<Long> listOfClinet=new HashSet<Long>()
		
		/*def premiumLYTD1 = LYTDList1.sum{
			if(it[0]!=null && it[0]!="")
				Double.parseDouble(it[0])
			else
				0.0
		}*/
		
		premium.LYTD = premium.LYTD ? premium.LYTD : 0
		premium.CYTD = premium.CYTD ? premium.CYTD : 0
		premium.LMTD = premium.LMTD ? premium.LMTD : 0
		premium.CMTD = premium.CMTD ? premium.CMTD : 0
		res.clients = clients
		res.premium = premium
		res.commission = commission
		res.result = "success"
		return res
	}
}