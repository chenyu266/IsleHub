package com.islehub.shop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.islehub.common.exception.BizException;
import com.islehub.shop.entity.Address;
import com.islehub.shop.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AddressService extends ServiceImpl<AddressMapper, Address> {

    public List<Address> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt));
    }

    @Transactional
    public void addAddress(Address address, Long userId) {
        address.setUserId(userId);
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        save(address);
    }

    @Transactional
    public void updateAddress(Address address, Long userId) {
        Address exist = getById(address.getId());
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BizException("地址不存在");
        }
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        // 只更新显式传入的字段，避免 request body 中的 null 误清空
        Address update = new Address();
        update.setId(address.getId());
        update.setUserId(userId);
        update.setReceiverName(address.getReceiverName());
        update.setReceiverPhone(address.getReceiverPhone());
        update.setProvince(address.getProvince());
        update.setCity(address.getCity());
        update.setDistrict(address.getDistrict());
        update.setDetail(address.getDetail());
        update.setIsDefault(address.getIsDefault());
        updateById(update);
    }

    public void deleteAddress(Long id, Long userId) {
        Address exist = getById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BizException("地址不存在");
        }
        removeById(id);
    }

    private void clearDefault(Long userId) {
        update(new LambdaUpdateWrapper<Address>()
                .set(Address::getIsDefault, 0)
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1));
    }
}
