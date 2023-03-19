package hellomyteam.hellomyteam.service;

import hellomyteam.hellomyteam.config.S3Uploader;
import hellomyteam.hellomyteam.dto.*;
import hellomyteam.hellomyteam.entity.*;
import hellomyteam.hellomyteam.entity.status.ConditionStatus;
import hellomyteam.hellomyteam.entity.status.MemberStatus;
import hellomyteam.hellomyteam.entity.status.team.AuthorityStatus;
import hellomyteam.hellomyteam.repository.FileUploadRepository;
import hellomyteam.hellomyteam.repository.MemberRepository;
import hellomyteam.hellomyteam.repository.TeamMemberInfoRepository;
import hellomyteam.hellomyteam.repository.TeamRepository;
import hellomyteam.hellomyteam.repository.custom.impl.FileUploadCustomImpl;
import hellomyteam.hellomyteam.repository.custom.impl.TeamCustomImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeamService {

    private final EntityManager em;
    private final TeamRepository teamRepository;
    private final TeamCustomImpl teamCustomImpl;
    private final TeamMemberInfoRepository teamMemberInfoRepository;
    private final FileUploadRepository fileUploadRepository;
    private final FileUploadCustomImpl fileUploadCustomImpl;
    private final S3Uploader s3Uploader;
    private final MemberRepository memberRepository;

    public Team createTeamWithAuthNo(TeamDto teamInfo) {
        int authNo = (int)(Math.random() * (9999 - 1000 + 1)) + 1000;
        Team team = Team.builder()
                .teamName(teamInfo.getTeamName())
                .oneIntro(teamInfo.getOneIntro())
                .detailIntro(teamInfo.getDetailIntro())
                .tacticalStyleStatus(teamInfo.getTacticalStyleStatus())
                .memberCount(1)
                .teamSerialNo(authNo)
                .build();
        teamRepository.save(team);
        return team;
    }

    public TeamMemberInfo teamMemberInfoSaveAuthLeader(Team team, Member member) {
        TeamMemberInfo teamMemberInfo = TeamMemberInfo.builder()
                .authority(AuthorityStatus.LEADER)
                .team(team)
                .member(member)
                .build();
        TeamMemberInfo savedteamMemberInfo = teamMemberInfoRepository.save(teamMemberInfo);
        return savedteamMemberInfo;
    }

    public List<TeamSearchDto> findTeamBySearchCond(String teamName, Integer teamSerialNo) {
        List<TeamSearchDto> team = teamCustomImpl.getInfoBySerialNoOrTeamName(teamName, teamSerialNo);
        return team;
    }

    public Team findTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("teamId가 누락되었습니다."));
        return team;
    }

    public TeamMemberInfo joinTeamAuthWait(Team team, Member member) {
        List<TeamMemberInfo> result = teamCustomImpl.findByTeamMember(team, member);

        if (result.size() > 0) {
            log.info("중복 가입신청 체크..." + String.valueOf(result));
            return null;
        }
        LocalDateTime currentDateTime = LocalDateTime.now();

        TeamMemberInfo teamMemberInfo = TeamMemberInfo.builder()
                .authority(AuthorityStatus.WAIT)
                .conditionStatus(ConditionStatus.PASSTION)
                .backNumber(0)
                .leftRightFoot("오른발")
                .conditionIndicator(50)
                .drinkingCapacity(1)
                .team(team)
                .member(member)
                .applyDate(currentDateTime)
                .build();
        teamMemberInfoRepository.save(teamMemberInfo);
        return teamMemberInfo;
    }

    public CommonResponse<?> acceptTeamMemberById(Long teamId, Long memberId) {
        Optional<Member> findMember = memberRepository.findById(memberId);
        MemberStatus memberStatus = findMember.get().getMemberStatus();
        if (!memberStatus.equals(MemberStatus.NORMAL)) {
            return CommonResponse.createError(memberStatus, "정상 회원이 아닙니다.");
        }

        TeamMemberInfo findTeamMemberInfo = teamMemberInfoRepository.findByTeamIdAndMemberId(teamId, memberId);
        if (!findTeamMemberInfo.getAuthority().equals(AuthorityStatus.WAIT)) {
            return CommonResponse.createError(findTeamMemberInfo.getAuthority(), "현재 소속팀의 팀원이므로, 가입 신청자가 아닙니다.");
        }

        TeamMemberInfo beforeTeamMemberInfo = em.find(TeamMemberInfo.class, findTeamMemberInfo.getId());
        beforeTeamMemberInfo.setAuthority(AuthorityStatus.TEAM_MEMBER);
        beforeTeamMemberInfo.setJoinDate(LocalDateTime.now());

        Team beforeTeam = em.find(Team.class, teamId);
        beforeTeam.setMemberCount(beforeTeam.getMemberCount() + 1);

        return CommonResponse.createSuccess(beforeTeamMemberInfo.getAuthority(), "팀원으로 반영되었습니다.");
    }

    public Team findTeamByTeamMemberId(TeamMemberIdDto teamMemberIdParam) {
        Team team = teamRepository.findById(teamMemberIdParam.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("teamId가 누락되었습니다."));
        return team;
    }

    public List saveLogo(MultipartFile multipartFile, Long teamId) throws IOException {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("teamId가 누락되었습니다."));

        if (!multipartFile.isEmpty()) {
            Map<String, String> storedFileURL = s3Uploader.upload(multipartFile, "teamLogo");
            String fileName = storedFileURL.get("fileName");
            String uploadImageUrl = storedFileURL.get("uploadImageUrl");

            Image image = Image.builder()
                    .team(team)
                    .imageUrl(uploadImageUrl)
                    .storeFilename(fileName)
                    .build();

            Boolean result = fileUploadRepository.existsImageByTeamId(teamId);
            //존재=true
            if (result) {
                fileUploadCustomImpl.updateLogoByTeam(teamId, image.getImageUrl(), image.getStoreFilename());
            } else {
                fileUploadRepository.save(image);
            }
        }

        List<Image> image =  fileUploadRepository.findImageByTeamId(teamId);
        return image;
    }

    public List<Image> deleteLogoByTeamId(Long teamId) {
        fileUploadCustomImpl.changeImageByTeamId(teamId);
        List<Image> image =  fileUploadRepository.findImageByTeamId(teamId);
        return image;
    }


    public Long deleteMemberByMemberId(Long teamId, MemberIdDto memberIdParam) {
        Long count = teamCustomImpl.deleteMemberByMemberId(teamId, memberIdParam.getMemberId());
        return count;
    }

    public Map<String, String> withDrawTeamByMemberId(Long teamId, MemberIdDto memberIdParam) {
        Map<String, String> param = new HashMap<>();
        AuthorityStatus authorityStatus = teamCustomImpl.getTeamMemberAuth(teamId, memberIdParam.getMemberId());

        if (!(authorityStatus.equals(AuthorityStatus.SUB_LEADER) || authorityStatus.equals(AuthorityStatus.TEAM_MEMBER))) {
            String stringResult = String.valueOf(authorityStatus);
            String template = "%s 의 권한일 경우 팀을 탈퇴 할 수 없습니다. 부팀장, 팀원으로 변경바랍니다.";
            String message = String.format(template, stringResult);
            param.put("message", message);
            param.put("authorityStatus", String.valueOf(authorityStatus));
            return param;
        }
        //팀 탈퇴
        teamCustomImpl.withDrawTeamByMemberId(teamId, memberIdParam.getMemberId());
        String stringResult = String.valueOf(authorityStatus);
        String template = "현재 권한: %s, 해당 팀을 탈퇴하였습니다.";
        String message = String.format(template, stringResult);
        param.put("message", message);
        param.put("authorityStatus", String.valueOf(authorityStatus));
        return param;
    }

    public TeamMemberInfoDto getTeamMemberInfo(Long teamMemberInfoId) {
        TeamMemberInfoDto teamMemberInfoDto = teamCustomImpl.findTeamMemberInfoById(teamMemberInfoId);
        return teamMemberInfoDto;
    }

    public TeamMemberInfoDto editTeamMemberInfo(Long teamMemberInfoId, TeamInfoUpdateDto teamInfoUpdateDto) {
        TeamMemberInfo teamMemberInfo = teamMemberInfoRepository.findTeamMemberInfoById(teamMemberInfoId);

        TeamMemberInfo findTeamMemberInfo = em.find(TeamMemberInfo.class, teamMemberInfoId);
        Member findMember = em.find(Member.class, teamMemberInfo.getMember().getId());

        findTeamMemberInfo.setAddress(teamInfoUpdateDto.getChangeAddress());
        findTeamMemberInfo.setConditionStatus(teamInfoUpdateDto.getChangeConditionStatus());
        findTeamMemberInfo.setBackNumber(teamInfoUpdateDto.getChangeBackNumber());
        findTeamMemberInfo.setMemberOneIntro(teamInfoUpdateDto.getChangeMemberOneIntro());
        findTeamMemberInfo.setLeftRightFoot(teamInfoUpdateDto.getChangeLeftRightFoot());
        findTeamMemberInfo.setConditionIndicator(teamInfoUpdateDto.getChangeConditionIndicator());
        findTeamMemberInfo.setDrinkingCapacity(teamInfoUpdateDto.getChangeDrinkingCapacity());
        findTeamMemberInfo.setPreferPosition(teamInfoUpdateDto.getChangePreferPosition());
        findMember.setBirthday(teamInfoUpdateDto.getChangeBirthday());
        findMember.setName(teamInfoUpdateDto.getChangeName());

        TeamMemberInfoDto result = teamCustomImpl.findTeamMemberInfoById(teamMemberInfoId);
        return result;
    }

    public CommonResponse<?> findAppliedTeamMember(Long teamMemberInfoId, Long teamId) {
        Optional<TeamMemberInfo> findTeamMemberInfo = teamMemberInfoRepository.findById(teamMemberInfoId);
        AuthorityStatus teamMemberStatus = findTeamMemberInfo.get().getAuthority();
        Long findTeamId = findTeamMemberInfo.get().getTeam().getId();

        if (findTeamId != teamId) {
            return CommonResponse.createSuccess(teamId, "가입한 팀이 아닙니다.");
        }

        if (teamMemberStatus.equals(AuthorityStatus.LEADER)) {
            List<ApplicantDto> applicantDto = teamCustomImpl.getApplyTeamMember(teamId);
            return CommonResponse.createSuccess(applicantDto, "리더의 경우 보여지는 팀 가입 신청 데이터입니다.");
        }
        return CommonResponse.createSuccess("데이터가 없습니다.");
    }

    public Page<TeamMemberInfosResDto> getTeamMemberInfos(Long teamId, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("created_date").descending());
        Page<TeamMemberInfosResDto> teamMemberInfosResDtos = teamCustomImpl.getTeamMemberInfoById(teamId, pageable);
        return teamMemberInfosResDtos;
    }

    public CommonResponse<?> getTeamMemberInfoId(Long teamId, Long memberId) {
        Optional<Team> team = teamRepository.findById(teamId);
        Optional<Member> member = memberRepository.findById(memberId);

        if (!team.isPresent()) {
            return CommonResponse.createError("가입한 팀이 없습니다. teamId를 확인해주세요.");
        }

        if (!member.isPresent()) {
            return CommonResponse.createError("가입한 회원이 아닙니다. memberId를 확인해주세요.");
        }

        boolean checkResult = teamMemberInfoRepository.existsByTeamIdAndMemberId(teamId, memberId);

        if (!checkResult) {
            return CommonResponse.createError("팀에 가입한 회원이 아닙니다. teamId와 memberId를 확인해주세요.");
        }

        Long teamMemberInfoId = teamCustomImpl.getTeamMemberInfoIdByIds(teamId, memberId);

        return CommonResponse.createSuccess(teamMemberInfoId, "teamMemberInfo_Id 값 success");
    }
}