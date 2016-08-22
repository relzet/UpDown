package com.relzet.service;

import com.relzet.dao.UserDocumentDao;
import com.relzet.model.UserDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("userDocumentService")
@Transactional
public class UserDocumentServiceImpl implements UserDocumentService{

	@Autowired
	UserDocumentDao dao;

	public UserDocument findById(int id) {
		return dao.findById(id);
	}

	public List<UserDocument> findAll() {
		return dao.findAll();
	}

	public List<UserDocument> findAllByUserId(int userId) {
		return dao.findAllByUserId(userId);
	}
	
	public void saveDocument(UserDocument document){
		dao.save(document);
	}

	public void deleteById(int id){
		dao.deleteById(id);
	}

	public List<UserDocument> findAllInFolder(int userId, int docId) {
		return dao.findAllInFolder(userId, docId);
	}

	public UserDocument findRootByUserId(int userId) {
		return dao.findRootByUserId(userId);
	}


}
