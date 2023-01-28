package HelloMyTeam.Hellomyteam.service;

import HelloMyTeam.Hellomyteam.dto.*;
import HelloMyTeam.Hellomyteam.entity.Member;
import HelloMyTeam.Hellomyteam.entity.Team;
import HelloMyTeam.Hellomyteam.entity.TeamMemberInfo;
import HelloMyTeam.Hellomyteam.entity.status.team.AuthorityStatus;
import HelloMyTeam.Hellomyteam.repository.TeamMemberInfoRepository;
import HelloMyTeam.Hellomyteam.repository.TeamRepository;
import HelloMyTeam.Hellomyteam.repository.custom.impl.TeamCustomImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.List;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamCustomImpl teamCustomImpl;
    private final TeamMemberInfoRepository teamMemberInfoRepository;

    public Team createTeamWithAuthNo(TeamParam teamInfo) {
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

    public void teamMemberInfoSaveAuthLeader(Team team, Member member) {
        TeamMemberInfo teamMemberInfo = TeamMemberInfo.builder()
                .authority(AuthorityStatus.LEADER)
                .team(team)
                .member(member)
                .build();
        teamMemberInfoRepository.save(teamMemberInfo);
    }

    public List<TeamSearchParam> findTeamBySearchCond(TeamSearchCond condition) {
        List<TeamSearchParam> team = teamCustomImpl.getInfoBySerialNoOrTeamName(condition);
        return team;
    }

    public Team findTeamById(TeamIdParam teamIdParam) {
        Team team = teamRepository.findById(teamIdParam.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("teamId가 누락되었습니다."));
        return team;
    }

    public boolean joinTeamAuthWait(Team team, Member member) {
        List<TeamMemberInfo> result = teamCustomImpl.findByTeamMember(team, member);

        if (result.size() > 0) {
            log.info("중복 가입신청 체크..." + String.valueOf(result));
            return false;
        }

        TeamMemberInfo teamMemberInfo = TeamMemberInfo.builder()
                .authority(AuthorityStatus.WAIT)
                .team(team)
                .member(member)
                .build();
        teamMemberInfoRepository.save(teamMemberInfo);
        return true;
    }

    public int acceptTeamMemberById(TeamMemberIdsParam teamMemberIdsParam) {
        //수락 전 auth 상태 체크
        Integer result = teamMemberInfoRepository.checkAuthWait(teamMemberIdsParam.getMemberId(), teamMemberIdsParam.getTeamId());
        if (result == 0 || result == null) {
            log.info("@result: " + result);
            return 0;
        }

        teamMemberInfoRepository.updateTeamMemberAuthById(teamMemberIdsParam.getMemberId(), teamMemberIdsParam.getTeamId());

        int countMember = teamMemberInfoRepository.getMemberCountByTeamId(teamMemberIdsParam.getTeamId());
        teamMemberInfoRepository.updateTeamCount(teamMemberIdsParam.getTeamId(), countMember);
        return result;
    }

    public Team findTeamByTeamMemberId(TeamMemberIdParam teamMemberIdParam) {
        Team team = teamRepository.findById(teamMemberIdParam.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("teamId가 누락되었습니다."));
        return team;
    }
}