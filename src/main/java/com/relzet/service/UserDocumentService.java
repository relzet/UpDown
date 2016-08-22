package com.relzet.service;

import com.relzet.model.UserDocument;

import java.util.List;

public interface UserDocumentService {

	UserDocument findById(int id);

	List<UserDocument> findAll();
	
	List<UserDocument> findAllByUserId(int id);
	
	void saveDocument(UserDocument document);
	
	void deleteById(int id);

	List<UserDocument> findAllInFolder(int userId, String path);
}
