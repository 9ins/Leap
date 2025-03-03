package org.chaostocosmos.leap.spring.repository;

import java.util.List;

import org.chaostocosmos.leap.spring.entity.Users;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * IUsersRespository
 * 
 * @author 9ins
 */
@Repository
public interface IUsersRepository extends CrudRepository<Users, Long> { 
    /**
     * Find by username
     * @param dataSource
     * @param username
     * @return
     */
    public List<Users> findByName(String username);
    /**
     * Find by user id
     * @param id 
     * @return
     */
    public Users findById(long id);
}
