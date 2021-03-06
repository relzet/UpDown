package com.relzet.controller;

import com.relzet.model.FileBucket;
import com.relzet.model.User;
import com.relzet.model.UserDocument;
import com.relzet.service.UserDocumentService;
import com.relzet.service.UserService;
import com.relzet.util.FileValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


@Controller
@RequestMapping("/")
public class AppController {

	@Autowired
	UserService userService;
	
	@Autowired
	UserDocumentService userDocumentService;
	
	@Autowired
	MessageSource messageSource;

	@Autowired
	FileValidator fileValidator;
	
	@InitBinder("fileBucket")
	protected void initBinder(WebDataBinder binder) {
	   binder.setValidator(fileValidator);
	}
	
	/**
	 * This method will list all existing users.
	 */
	@RequestMapping(value = { "/", "/list" }, method = RequestMethod.GET)
	public String listUsers(ModelMap model) {

		List<User> users = userService.findAllUsers();
		model.addAttribute("users", users);
		return "userslist";
	}

	/**
	 * This method will provide the medium to add a new user.
	 */
	@RequestMapping(value = { "/newuser" }, method = RequestMethod.GET)
	public String newUser(ModelMap model) {
		User user = new User();
		model.addAttribute("user", user);
		model.addAttribute("edit", false);
		return "registration";
	}

	/**
	 * This method will be called on form submission, handling POST request for
	 * saving user in database. It also validates the user input
	 */
	@RequestMapping(value = { "/newuser" }, method = RequestMethod.POST)
	public String saveUser(@Valid User user, BindingResult result,
			ModelMap model) {

		if (result.hasErrors()) {
			return "registration";
		}

		/*
		 * Preferred way to achieve uniqueness of field [sso] should be implementing custom @Unique annotation 
		 * and applying it on field [sso] of Model class [User].
		 * 
		 * Below mentioned peace of code [if block] is to demonstrate that you can fill custom errors outside the validation
		 * framework as well while still using internationalized messages.
		 * 
		 */
		if(!userService.isUserSSOUnique(user.getId(), user.getSsoId())){
			FieldError ssoError =new FieldError("user","ssoId",messageSource.getMessage("non.unique.ssoId", new String[]{user.getSsoId()}, Locale.getDefault()));
		    result.addError(ssoError);
			return "registration";
		}
		
		userService.saveUser(user);
		
		model.addAttribute("user", user);
		model.addAttribute("success", "User " + user.getFirstName() + " "+ user.getLastName() + " registered successfully");

		userDocumentService.saveDocument(createFolder(user, "ROOT"));
		//return "success";
		return "registrationsuccess";
	}




	/**
	 * This method will provide the medium to update an existing user.
	 */
	@RequestMapping(value = { "/edit-user-{ssoId}" }, method = RequestMethod.GET)
	public String editUser(@PathVariable String ssoId, ModelMap model) {
		User user = userService.findBySSO(ssoId);
		model.addAttribute("user", user);
		model.addAttribute("edit", true);
		return "registration";
	}
	
	/**
	 * This method will be called on form submission, handling POST request for
	 * updating user in database. It also validates the user input
	 */
	@RequestMapping(value = { "/edit-user-{ssoId}" }, method = RequestMethod.POST)
	public String updateUser(@Valid User user, BindingResult result,
			ModelMap model, @PathVariable String ssoId) {

		if (result.hasErrors()) {
			return "registration";
		}

		userService.updateUser(user);

		model.addAttribute("success", "User " + user.getFirstName() + " "+ user.getLastName() + " updated successfully");
		return "registrationsuccess";
	}

	
	/**
	 * This method will delete an user by it's SSOID value.
	 */
	@RequestMapping(value = { "/delete-user-{ssoId}" }, method = RequestMethod.GET)
	public String deleteUser(@PathVariable String ssoId) {
		userService.deleteUserBySSO(ssoId);
		return "redirect:/list";
	}
	

	
	@RequestMapping(value = { "/add-document-{userId}" }, method = RequestMethod.GET)
	public String addDocuments(@PathVariable int userId, ModelMap model) {

		UserDocument doc = userDocumentService.findRootByUserId(userId);
		return "redirect:/open-folder-"+userId+"-"+doc.getId();
	}


	@RequestMapping(value = { "/search-{userId}-{docId}" }, method = RequestMethod.GET)
	public String search(@PathVariable int userId, @PathVariable int docId, @RequestParam("target") String target, ModelMap model) throws IOException {
		User user = userService.findById(userId);
		model.addAttribute("user", user);

		FileBucket fileModel = new FileBucket();
		model.addAttribute("fileBucket", fileModel);

		List<UserDocument> folders = userDocumentService.searchFoldersInFolder(userId, docId, target);
		model.addAttribute("folders", folders);

		List<UserDocument> documents = userDocumentService.searchDocsInFolder(userId, docId, target);
		model.addAttribute("documents", documents);


		model.addAttribute("currentFolder", userDocumentService.findById(docId));

		return "managedocuments";
	}


	

	@RequestMapping(value = { "/download-document-{userId}-{docId}" }, method = RequestMethod.GET)
	public String downloadDocument(@PathVariable int userId, @PathVariable int docId, HttpServletResponse response) throws IOException {
		UserDocument document = userDocumentService.findById(docId);
		response.setContentType(document.getType());
        response.setContentLength(document.getContent().length);
//        response.setHeader("Content-Disposition","inline; filename=\"" + document.getName() +"\"");     //open in browser
		response.setHeader("Content-Disposition","attachment; filename=\"" + document.getName() +"\""); //download file


		FileCopyUtils.copy(document.getContent(), response.getOutputStream());
 
 		return "redirect:/add-document-"+userId;
	}

	@RequestMapping(value = { "/preview-document-{userId}-{docId}" }, method = RequestMethod.GET)
	public String previewDocument(@PathVariable int userId, @PathVariable int docId, HttpServletResponse response) throws IOException {
		UserDocument document = userDocumentService.findById(docId);
		response.setContentType(document.getType());
		response.setContentLength(document.getContent().length);
        response.setHeader("Content-Disposition","inline; filename=\"" + document.getName() +"\"");     //open in browser
//		response.setHeader("Content-Disposition","attachment; filename=\"" + document.getName() +"\""); //download file


		FileCopyUtils.copy(document.getContent(), response.getOutputStream());

		return "redirect:/add-document-"+userId;
	}


	//// TODO: 22.08.2016
	//// TODO: 22.08.2016
	@RequestMapping(value = { "/open-folder-{userId}-{docId}" }, method = RequestMethod.GET)
	public String openFolder(@PathVariable int userId, @PathVariable int docId, ModelMap model, @ModelAttribute("folderNameError") String folderNameError, @ModelAttribute("folderUniqueError") String folderUniqueError) throws IOException {
		model.addAttribute("folderNameError", folderNameError);
		model.addAttribute("folderUniqueError", folderUniqueError);

		User user = userService.findById(userId);
		model.addAttribute("user", user);

		FileBucket fileModel = new FileBucket();
		model.addAttribute("fileBucket", fileModel);

		List<UserDocument> folders = userDocumentService.findFoldersInFolder(userId, docId);
		model.addAttribute("folders", folders);

		List<UserDocument> documents = userDocumentService.findDocsInFolder(userId, docId);
		model.addAttribute("documents", documents);



		model.addAttribute("currentFolder", userDocumentService.findById(docId));

		return "managedocuments";

	}

	@RequestMapping(value = { "/filter-{userId}-{docId}" }, method = RequestMethod.GET)
	public String openFolder(@PathVariable int userId, @RequestParam("filters") String[] filters, @PathVariable int docId, ModelMap model) throws IOException {
		User user = userService.findById(userId);
		model.addAttribute("user", user);

		FileBucket fileModel = new FileBucket();
		model.addAttribute("fileBucket", fileModel);


		List<UserDocument> documents = userDocumentService.filterDocsInFolder(userId, docId, filters);
		model.addAttribute("documents", documents);


		model.addAttribute("currentFolder", userDocumentService.findById(docId));

		return "managedocuments";

	}
	@RequestMapping(value = { "/delete-document-{userId}-{docId}-{currentFolderId}" }, method = RequestMethod.GET)
	public String deleteDocument(@PathVariable int userId, @PathVariable int docId, @PathVariable int currentFolderId) {

		userDocumentService.deleteById(docId, currentFolderId);
//		return "redirect:/add-document-"+userId;
		return "redirect:/open-folder-"+userId+"-"+currentFolderId;

	}

	//@Delete folder
	@RequestMapping(value = { "/delete-folder-{userId}-{docId}" }, method = RequestMethod.GET)
	public String deleteDocument(@PathVariable int userId, @PathVariable int docId) {
		userDocumentService.deleteFolderById(docId);

		return "redirect:/add-document-"+userId;
	}

	@RequestMapping(value = { "/add-document-{userId}-{docId}" }, method = RequestMethod.POST)
	public String uploadDocument(@Valid FileBucket fileBucket, BindingResult result, ModelMap model, @PathVariable int userId, @PathVariable int docId) throws IOException{
		
		if (result.hasErrors()) {
			System.out.println("validation errors");
			User user = userService.findById(userId);
			model.addAttribute("user", user);

			List<UserDocument> documents = userDocumentService.findAllByUserId(userId);
			model.addAttribute("documents", documents);
			
			return "redirect:/add-document-"+userId;
		} else {
			
			System.out.println("Fetching file");
			
			User user = userService.findById(userId);
			model.addAttribute("user", user);

			saveDocument(fileBucket, user, docId);

			return "redirect:/open-folder-"+userId+"-"+docId;
		}
	}

	// TODO: 21.08.2016

	@RequestMapping(value = { "/create-folder-{userId}-{docId}" }, method = RequestMethod.POST)
	public String createFolder(ModelMap model, @PathVariable int userId, @PathVariable int docId, @RequestParam("folderName") String folderName, RedirectAttributes redirectAttrs) throws IOException{

		if (folderName.contains(".")||folderName.contains("/")||folderName.contains("\\")){
			redirectAttrs.addFlashAttribute("folderNameError", "A Foldername cannot contain any of the following characters:\n" +
					"\\ / .");
			return "redirect:/open-folder-"+userId+"-"+docId;
		}
		if (userDocumentService.checkFolderNameUnique(userId, docId, folderName)){
			redirectAttrs.addFlashAttribute("folderUniqueError", "Folder \""+folderName+"\" already exists");
			return "redirect:/open-folder-"+userId+"-"+docId;
		}

		User user = userService.findById(userId);
		model.addAttribute("user", user);


		userDocumentService.saveDocument(createFolder(
				user,
				folderName,
				userDocumentService.findById(docId).getDescription()+"."+folderName
		));

//			return "redirect:/add-document-"+userId;
		return "redirect:/open-folder-"+userId+"-"+docId;

	}
	private void saveDocument(FileBucket fileBucket, User user, int docId) throws IOException{
		
		UserDocument document = new UserDocument();
		UserDocument folder = userDocumentService.findById(docId);

		MultipartFile multipartFile = fileBucket.getFile();
		if (multipartFile.getContentType().contains("video")) document.setGlyphicon("-video-"); else
		if (multipartFile.getContentType().contains("image")) document.setGlyphicon("-picture-"); else
		if (multipartFile.getContentType().contains("audio")) document.setGlyphicon("-audio-"); else
		if (multipartFile.getContentType().contains("zip")) document.setGlyphicon("-zip-"); else
		if (multipartFile.getContentType().contains("pdf")) document.setGlyphicon("-pdf-"); else
		if (multipartFile.getContentType().contains("text")|| multipartFile.getContentType().contains("officedocument")||multipartFile.getContentType().contains("msword")) document.setGlyphicon("-text-"); else
			document.setGlyphicon("-");

		folder.setSize(folder.getSize()+(int)multipartFile.getSize()/1000);
		folder.setFilesCounter(folder.getFilesCounter()+1);

		document.setName(multipartFile.getOriginalFilename());
		document.setDescription(folder.getDescription()+"."+multipartFile.getOriginalFilename());
		document.setType(multipartFile.getContentType());
		document.setContent(multipartFile.getBytes());
		document.setUser(user);
		document.setSize((int)multipartFile.getSize()/1000);

		userDocumentService.updateDocument(folder);
		userDocumentService.saveDocument(document);
	}

	private UserDocument createFolder(User user , String folderName) {
		return createFolder(user, folderName, "ROOT");
	}

	private UserDocument createFolder(User user , String folderName, String parentFolder) {
		UserDocument doc = new UserDocument(folderName, parentFolder, "folder", new byte[]{0}, user);
		doc.setFolder(true);
		return doc;
	}
	
}
