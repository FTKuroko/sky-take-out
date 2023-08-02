package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.AddressBook;

import java.util.List;

/**
 * @author Kuroko
 * @description
 * @date 2023/8/1 15:33
 */
public interface AddressBookService extends IService<AddressBook> {
    /**
     * 动态查询地址列表
     * @param addressBook
     * @return
     */
    List<AddressBook> listAddress(AddressBook addressBook);

    /**
     * 新增地址
     * @param addressBook
     */
    void saveAddress(AddressBook addressBook);

    /**
     * 根据 id 查询地址
     * @param id
     * @return
     */
    AddressBook getAddressById(Long id);

    /**
     * 更新地址信息
     * @param addressBook
     */
    void updateAddress(AddressBook addressBook);

    /**
     * 设置默认地址
     * @param addressBook
     */
    void setDefaultAddress(AddressBook addressBook);

    /**
     * 根据 id 删除地址
     * @param id
     */
    void deleteAddressById(Long id);

    /**
     * 获取默认地址
     * @return
     */
    AddressBook getDefaultAddress();

}
