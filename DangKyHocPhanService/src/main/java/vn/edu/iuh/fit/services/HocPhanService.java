package vn.edu.iuh.fit.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.edu.iuh.fit.dtos.request.HocKyNienGiamRequest;
import vn.edu.iuh.fit.dtos.request.HocPhanRequest;
import vn.edu.iuh.fit.dtos.request.HocPhanTheoNienGiamRequest;
import vn.edu.iuh.fit.dtos.response.HocPhanResponse;
import vn.edu.iuh.fit.dtos.response.HocPhanTheoNienGiamResponse;
import vn.edu.iuh.fit.models.*;
import vn.edu.iuh.fit.repositories.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class HocPhanService {
    @Autowired
    private KhoaRepository khoaRepository;
    @Autowired
    private NganhRepository nganhRepository;
    @Autowired
    private HocPhanRepository hocPhanRepository;
    @Autowired
    private HocPhanTienQuyetRepository hocPhanTienQuyetRepository;
    @Autowired
    private HocKyNienGiamRepository hocKyNienGiamRepository;
    @Autowired
    private HocPhanTheoNienGiamRepository hocPhanTheoNienGiamRepository;
    @Autowired
    private NhomHocPhanTuChonRepository nhomHocPhanTuChonRepository;
    @Autowired
    private RestTemplate restTemplate;
    private String urlKQHT = "http://localhost:9000/KetQuaHocTapService/HocPhan/";

    public HocPhan createHocPhan(HocPhanRequest hocPhanRequest) {
        if ((hocPhanRequest.getMaKhoa() != 0 && !khoaRepository.existsById(hocPhanRequest.getMaKhoa()))
                || (hocPhanRequest.getMaNganh() != 0 && !nganhRepository.existsById(hocPhanRequest.getMaNganh()))) {
            return null;    // mã ngành hoặc mã khoa không tồn tại
        }
        HocPhan hocPhan = new HocPhan(hocPhanRequest.getTenHocPhan(),
                hocPhanRequest.getMaNganh() == 0 ? null : new Nganh(hocPhanRequest.getMaNganh()),
                hocPhanRequest.getMaKhoa() == 0 ? null : new Khoa(hocPhanRequest.getMaKhoa()),
                hocPhanRequest.getSoTinChiLyThuyet(),
                hocPhanRequest.getSoTinChiThucHanh());
        HocPhanResponse hocPhanResponse = new HocPhanResponse(hocPhanRepository.save(hocPhan));
        restTemplate.postForObject(urlKQHT +"createHocPhan", hocPhanResponse, Void.class);
        return hocPhan;
    }

    public HocPhan getHocPhanById(long id) {
        return hocPhanRepository.findById(id).orElse(null);
    }

    public boolean deleteHocPhan(long id) {
        if (!hocPhanRepository.existsById(id))
            return false;       // không tìm thấy id
        if (hocPhanTheoNienGiamRepository.existsByHocPhan(new HocPhan(id)))
            return false;       // học phần đã được sử dụng trong học kỳ niên giảm
        if (hocPhanTienQuyetRepository.existsByMaHocPhan(id))
            return false;       // học phần đã được sử dụng trong học phần tiên quyết
        hocPhanRepository.deleteById(id);
        return true;
    }

    public HocPhanTienQuyet setHocPhanTienQuyet(long maHocPhan, long maHocPhanTienQuyet) {
        if (!existsHocPhan(maHocPhan) || !existsHocPhan(maHocPhanTienQuyet))
            return null;       // không tìm thấy mã học phần hoặc mã học phần tiên quyết
        HocPhanTienQuyet hptq = new HocPhanTienQuyet(maHocPhan, maHocPhanTienQuyet);
        boolean res = hocPhanTienQuyetRepository.existsByMaHocPhanAndMaHocPhanTienQuyet(maHocPhan, maHocPhanTienQuyet);
        if (res)
            return null;    // đã tồn tại quan hệ
        return hocPhanTienQuyetRepository.save(hptq);
    }

    public List<HocPhanTienQuyet> getHocPhanTienQuyet(long id) {
        if (!existsHocPhan(id))
            return null;    // không tìm thấy mã học phần
        return hocPhanTienQuyetRepository.findByMaHocPhan(id);
    }

    public boolean deleteHocPhanTienQuyet(long maHocPhan, long maHocPhanTienQuyet) {
        if (!existsHocPhan(maHocPhan) || !existsHocPhan(maHocPhanTienQuyet))
            return false;       // không tìm thấy mã học phần hoặc mã học phần tiên quyết
        HocPhanTienQuyet hptq = hocPhanTienQuyetRepository.findByMaHocPhanAndMaHocPhanTienQuyet(maHocPhan, maHocPhanTienQuyet);
        if (hptq != null)
            hocPhanTienQuyetRepository.delete(hptq);
        return true;
    }

    public HocKyNienGiam createHocKyNienGiam(HocKyNienGiamRequest hocKyNienGiamRequest) {
        HocKyNienGiam hocKyNienGiam = new HocKyNienGiam(hocKyNienGiamRequest.getMaNganh(), hocKyNienGiamRequest.getKhoa(), hocKyNienGiamRequest.getHocKy());
        if (hocKyNienGiamRepository.existsByNganhAndKhoaAndHocKy(hocKyNienGiam.getNganh(), hocKyNienGiam.getKhoa(), hocKyNienGiam.getHocKy()))
            return null;    // đã tồn tại học kỳ niên giảm
        if (!nganhRepository.existsById(hocKyNienGiamRequest.getMaNganh()))
            return null;    // mã ngành không tồn tại
        hocKyNienGiamRepository.save(hocKyNienGiam);
        if (hocKyNienGiamRequest.getHocPhanTheoNienGiam()==null)
            return hocKyNienGiam;
        for (Long id : hocKyNienGiamRequest.getHocPhanTheoNienGiam()) {
            HocPhanTheoNienGiam hocPhanTheoNienGiam = new HocPhanTheoNienGiam(new HocPhan(id), hocKyNienGiam);
            hocPhanTheoNienGiamRepository.save(hocPhanTheoNienGiam);        //thêm học phần vào học kỳ niên giảm
        }
        return hocKyNienGiam; // chỉ tạo học kỳ
    }

    public boolean themHocPhanVaoNienGiam(HocPhanTheoNienGiamRequest hocPhanTheoNienGiamRequest) {
        if (!hocPhanRepository.existsById(hocPhanTheoNienGiamRequest.getMaHocPhan()))
            return false;       // không tìm thấy mã học phần
        if (!hocKyNienGiamRepository.existsById(hocPhanTheoNienGiamRequest.getMaHocKyNienGiam()))
            return false;       // không tìm thấy mã học kỳ niên giảm
        HocPhan hocPhan = new HocPhan(hocPhanTheoNienGiamRequest.getMaHocPhan());
        HocKyNienGiam hocKyNienGiam = new HocKyNienGiam(hocPhanTheoNienGiamRequest.getMaHocKyNienGiam());
        HocPhanTheoNienGiam hocPhanTheoNienGiam = new HocPhanTheoNienGiam(hocPhanTheoNienGiamRequest.getMaHocPhan(), hocPhanTheoNienGiamRequest.getMaHocKyNienGiam());
        if (!hocPhanTheoNienGiamRepository.existsByHocPhanAndHocKyNienGiam(hocPhan, hocKyNienGiam))
            hocPhanTheoNienGiamRepository.save(hocPhanTheoNienGiam);
        return true;
    }

    public List<HocPhanTheoNienGiamResponse> getNienGiamByNganhAndKhoa(long maNganh, int khoa) {
        List<HocKyNienGiam> hocKyNienGiamList = hocKyNienGiamRepository.findByNganhAndKhoa(maNganh, khoa);
        if (hocKyNienGiamList == null)
            return null;    // không tìm thấy học kỳ niên giảm
        List<HocPhanTheoNienGiamResponse> hocPhanTheoNienGiamResponseList = new ArrayList<>();
        for (HocKyNienGiam hocKyNienGiam : hocKyNienGiamList) {
            HocPhanTheoNienGiamResponse hocPhanTheoNienGiamResponse = getHocPhanTheoNienGiamResponse(hocKyNienGiam);
            hocPhanTheoNienGiamResponseList.add(hocPhanTheoNienGiamResponse);
        }
        System.out.println(hocPhanTheoNienGiamResponseList);
        return hocPhanTheoNienGiamResponseList;
    }

    private static HocPhanTheoNienGiamResponse getHocPhanTheoNienGiamResponse(HocKyNienGiam hocKyNienGiam) {
        HocPhanTheoNienGiamResponse hocPhanTheoNienGiamResponse = new HocPhanTheoNienGiamResponse();
        hocPhanTheoNienGiamResponse.setMaHocKyNienGiam(hocKyNienGiam.getMaHocKyNienGiam());
        hocPhanTheoNienGiamResponse.setHocKy(hocKyNienGiam.getHocKy());
        for (HocPhanTheoNienGiam hocPhanTheoNienGiam : hocKyNienGiam.getHocPhanTheoNienGiam()){
            HocPhanResponse hocPhanResponse = new HocPhanResponse(hocPhanTheoNienGiam.getHocPhan());
            hocPhanTheoNienGiamResponse.addHocPhanResponse(hocPhanResponse);
        }
        return hocPhanTheoNienGiamResponse;
    }


    public List<HocKyNienGiam> getHocKyNienGiamByNganhAndKhoa(long maNganh, long khoa) {
        return hocKyNienGiamRepository.findByNganhAndKhoa(new Nganh(maNganh), khoa);
    }

    public List<HocKyNienGiam> getHocKyNienGiamByNganhAndKhoaAndHocKy(long maNganh, long khoa, int hocKy) {
        return hocKyNienGiamRepository.findByNganhAndKhoaAndHocKy(new Nganh(maNganh), khoa, hocKy);
    }

    public boolean existsHocPhan(long id) {
        return hocPhanRepository.existsById(id);
    }

    public List<HocPhanResponse> findDanhSachHocPhanSinhVienCoTheDangKy( long maNganh, int khoa) {
        List<HocPhanResponse> hocPhanResponses = new ArrayList<>();
        List<HocPhan> hocPhans = hocPhanRepository.findHocPhanSinhVienDangKy(maNganh, khoa);
        if (hocPhans == null)
            return null;    // không tìm thấy học phần
        for (HocPhan hocPhan : hocPhans) {
            HocPhanResponse hocPhanResponse = new HocPhanResponse(hocPhan);
            hocPhanResponses.add(hocPhanResponse);
        }
        return hocPhanResponses;
    }

}
