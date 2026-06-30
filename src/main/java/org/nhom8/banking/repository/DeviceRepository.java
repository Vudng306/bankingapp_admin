package org.nhom8.banking.repository;

import org.nhom8.banking.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUserIdOrderByLastLoginAtDesc(Long userId);

    Optional<Device> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<Device> findByIdAndUserId(Long id, Long userId);

    /** Dùng bởi FcmDispatchAspect: lấy tất cả thiết bị còn hoạt động có push token */
    List<Device> findByUserIdAndActiveTrueAndPushTokenIsNotNull(Long userId);
}
