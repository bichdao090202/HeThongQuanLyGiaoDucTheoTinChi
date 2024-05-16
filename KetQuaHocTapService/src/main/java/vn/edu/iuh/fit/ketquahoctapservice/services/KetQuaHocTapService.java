package vn.edu.iuh.fit.ketquahoctapservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.ketquahoctapservice.dtos.request.DiemLopHocPhanRequest;
import vn.edu.iuh.fit.ketquahoctapservice.dtos.request.KetQuaSinhVien;
import vn.edu.iuh.fit.ketquahoctapservice.dtos.res.KetQuaHocTapSinhVienDetail;
import vn.edu.iuh.fit.ketquahoctapservice.model.HocPhan;
import vn.edu.iuh.fit.ketquahoctapservice.model.KetQuaHocKy;
import vn.edu.iuh.fit.ketquahoctapservice.model.KetQuaHocPhan;
import vn.edu.iuh.fit.ketquahoctapservice.repositories.HocPhanRepository;
import vn.edu.iuh.fit.ketquahoctapservice.repositories.KetQuaHocKyRepository;
import vn.edu.iuh.fit.ketquahoctapservice.repositories.KetQuaHocPhanRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class KetQuaHocTapService {
    @Autowired
    private KetQuaHocPhanRepository ketQuaHocPhanRepository;
    @Autowired
    private KetQuaHocKyRepository ketQuaHocKyRepository;
    @Autowired
    private HocPhanRepository hocPhanRepository;

    public KetQuaHocPhan nhapDiemChoSinhVien(KetQuaSinhVien ketQuaSinhVien, long maHocPhan, int hocKy) {
        HocPhan hocPhan = hocPhanRepository.findById(maHocPhan).orElse(null);
        if (hocPhan == null)
            return null;    // mã học phần không tồn tại
        return createKetQuaHocPhan(ketQuaSinhVien, hocKy,hocPhan);
    }

    public List<Long> nhapDiemChoLopHocPhan(DiemLopHocPhanRequest diemLopHocPhanRequest) {
        HocPhan hocPhan = hocPhanRepository.findById(diemLopHocPhanRequest.getMaHocPhan()).orElse(null);
        if (hocPhan == null)
            return null;    // mã học phần không tồn tại
        List<Long> danhSachMSSVDiemKhongHopLe = new ArrayList<>();
        for (KetQuaSinhVien ketQuaSinhVien : diemLopHocPhanRequest.getListKetQuaSinhVien()) {
            if (createKetQuaHocPhan(ketQuaSinhVien, diemLopHocPhanRequest.getHocKy(),hocPhan)==null)
                danhSachMSSVDiemKhongHopLe.add(ketQuaSinhVien.getMaSinhVien());
        }
        return danhSachMSSVDiemKhongHopLe;
    }

    private KetQuaHocPhan createKetQuaHocPhan(KetQuaSinhVien ketQuaSinhVien, int hocKy, HocPhan hocPhan){
        if (ketQuaSinhVien.getListDiem().size() != 5 && ketQuaSinhVien.getListDiem().size() != 8)
            return null;    // số lượng điểm không hợp lệ
        System.out.println("1");
        if (hocPhan.getSoTinChiThucHanh() > 0 && ketQuaSinhVien.getListDiem().size() == 5 ||
                hocPhan.getSoTinChiThucHanh() == 0 && ketQuaSinhVien.getListDiem().size() == 8)
            return null;    // số lượng điểm không hợp lệ
        System.out.println("2");
        KetQuaHocPhan ketQuaHocPhan = new KetQuaHocPhan(ketQuaSinhVien,hocPhan, hocKy);
        if (!ketQuaHocPhan.checkDiemHopLe())
            return null;    // điểm không hợp lệ
        System.out.println("3");
        ketQuaHocPhan.setHocPhan(hocPhan);
        KetQuaHocPhan tmp = ketQuaHocPhanRepository.findByHocPhanAndMaSinhVien(hocPhan, ketQuaSinhVien.getMaSinhVien());
        if (tmp != null)
            ketQuaHocPhan.setMaKetQuaHocPhan(tmp.getMaKetQuaHocPhan());     // update kết quả cũ
        System.out.println("4");
        ketQuaHocPhan.setHocPhan(hocPhan);
        createKetQuaHocKy(ketQuaSinhVien.getMaSinhVien(), hocKy, ketQuaHocPhan);
        return ketQuaHocPhanRepository.save(ketQuaHocPhan);
    }

    public KetQuaHocTapSinhVienDetail getKetQuaSinhVien(long maSinhVien) {
        List<KetQuaHocKy> ketQuaHocKyList = ketQuaHocKyRepository.findByMaSinhVien(maSinhVien);
        System.out.println(ketQuaHocKyList);
        if (ketQuaHocKyList.isEmpty())
            return null;    // sinh viên chưa có kết quả học tập
        KetQuaHocTapSinhVienDetail ketQuaHocTapSinhVienDetail = new KetQuaHocTapSinhVienDetail(ketQuaHocKyList);
        return ketQuaHocTapSinhVienDetail;
    }

    private void createKetQuaHocKy(long maSinhVien, int hocKy, KetQuaHocPhan ketQuaHocPhan){
        KetQuaHocKy ketQuaHocKy = ketQuaHocKyRepository.findByMaSinhVienAndHocKy(maSinhVien, hocKy);
        if (ketQuaHocKy == null){
            ketQuaHocKy = new KetQuaHocKy(maSinhVien, ketQuaHocPhan, hocKy);
            ketQuaHocKyRepository.save(ketQuaHocKy);
        } else {
            List<KetQuaHocPhan> ketQuaHocPhanList = ketQuaHocPhanRepository.findByKetQuaHocKy(new KetQuaHocKy(maSinhVien, hocKy));
            if (!ketQuaHocPhanList.contains(ketQuaHocPhan))
                ketQuaHocPhanList.add(ketQuaHocPhan);
            ketQuaHocKy.updateKeQuaHocKy(ketQuaHocPhanList);
            ketQuaHocKyRepository.save(ketQuaHocKy);
        }
    }
}
