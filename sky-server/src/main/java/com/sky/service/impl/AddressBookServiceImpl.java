package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.service.AddressBookService;
import com.sky.mapper.AddressBookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author 12548
* @description 针对表【address_book(地址簿)】的数据库操作Service实现
* @createDate 2024-08-28 12:48:32
*/
@Service
@RequiredArgsConstructor
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook>
    implements AddressBookService{

    private final AddressBookMapper addressBookMapper;

    @Override
    @Transactional
    public void setDefault(AddressBook addressBook) {
        LambdaUpdateWrapper<AddressBook> updateWrapper = new LambdaUpdateWrapper<AddressBook>();
        updateWrapper.set(AddressBook::getIsDefault,0).eq(AddressBook::getUserId, BaseContext.getCurrentId()).eq(AddressBook::getIsDefault,1);
        addressBookMapper.update(updateWrapper);
        addressBook.setIsDefault(1);
        addressBookMapper.updateById(addressBook);
    }
}




