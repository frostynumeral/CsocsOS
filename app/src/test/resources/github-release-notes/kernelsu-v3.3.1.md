## What's Changed
* docs: update version references to 3.3.0 by @5ec1cff in https://github.com/tiann/KernelSU/pull/3667
* refactor(kernel, ksud, manager): Switch webview zygote umount to allowlist by @aviraxp in https://github.com/tiann/KernelSU/pull/3666
* fix(actions): upload release artifacts correctly by @5ec1cff in https://github.com/tiann/KernelSU/pull/3671
* docs, manager: update Indonesian translations by @Neebe3289 in https://github.com/tiann/KernelSU/pull/3656
* fix(kernel): Preserve a ebitmap's size for each policy type by @aviraxp in https://github.com/tiann/KernelSU/pull/3680
* manager: update Chinese (Traditional Han script, Hong Kong) translations by @tpnonthealps in https://github.com/tiann/KernelSU/pull/3685
* feat(ksud): Implement SIGSYS handler by @aviraxp in https://github.com/tiann/KernelSU/pull/3677
* fix(kernel, uapi, ksud): make fd wrapper work with app profile by @Dr-TSNG in https://github.com/tiann/KernelSU/pull/3679
* build(deps): bump the npm group in /website with 14 updates by @dependabot[bot] in https://github.com/tiann/KernelSU/pull/3676
* build(deps): bump the crates group with 7 updates by @dependabot[bot] in https://github.com/tiann/KernelSU/pull/3675
* build(deps): bump actions/setup-java from 5 to 6 in the actions group by @dependabot[bot] in https://github.com/tiann/KernelSU/pull/3673
* feat: new version matching detection mechanism by @5ec1cff in https://github.com/tiann/KernelSU/pull/3516
* feat(resetprop): support update long value for ro props by @5ec1cff in https://github.com/tiann/KernelSU/pull/3696
* fix(kernel): Reject all signature block id except v2 by @aviraxp in https://github.com/tiann/KernelSU/pull/3700
* kernel: retain capabilities across execve for non-root profiles by @u9521 in https://github.com/tiann/KernelSU/pull/3710
* fix(manager): Use MediaStore to download modules by @aviraxp in https://github.com/tiann/KernelSU/pull/3707
* build(deps): bump the maven group in /manager with 5 updates by @dependabot[bot] in https://github.com/tiann/KernelSU/pull/3727
* manager: use LazyColumn in ColorPaletteScreenMaterial by @KOWX712 in https://github.com/tiann/KernelSU/pull/3730
* manager: Support customizing module description collapse line count by @HChenX in https://github.com/tiann/KernelSU/pull/3703
* build(deps): migrate dependencies to new repos by @5ec1cff in https://github.com/tiann/KernelSU/pull/3723
* fix(kernel): open file without touching fsnotify and atime while scanning manager apks by @5ec1cff in https://github.com/tiann/KernelSU/pull/3733
* [FIX] Miscellaneous build errors in syscall_hook_manager and arm64 patch_memory by @0ctobot in https://github.com/tiann/KernelSU/pull/3738
* kernel: util: wire up ksyscall by @backslashxx in https://github.com/tiann/KernelSU/pull/3742
* kernel: fix avtab iteration macro by @XiaoTong6666 in https://github.com/tiann/KernelSU/pull/3701
* fix(manager): Prevent exception if no root access by @aviraxp in https://github.com/tiann/KernelSU/pull/3741
* fix(ksud): wait for service death before post-fs-data when soft reboot by @5ec1cff in https://github.com/tiann/KernelSU/pull/3744
* kernel: fix RCU-protected SELinux policy access by @XiaoTong6666 in https://github.com/tiann/KernelSU/pull/3746
* fix(manager): Unexport magica receiver by @aviraxp in https://github.com/tiann/KernelSU/pull/3747
* kernel: Add Android riscv64 support with LKM by @yujincheng08 in https://github.com/tiann/KernelSU/pull/3750
* fix(ksud): use waitsys to wait when soft reboot by @5ec1cff in https://github.com/tiann/KernelSU/pull/3754
* refactor(ksud): update waitsys and refactor logic by @5ec1cff in https://github.com/tiann/KernelSU/pull/3755
* feat(ksud): add timeout for boot stage scripts by @5ec1cff in https://github.com/tiann/KernelSU/pull/3797
* Sepolicy by @aviraxp in https://github.com/tiann/KernelSU/pull/3799
* fix(kernel): Avoid repeatedly triggering service stage by @aviraxp in https://github.com/tiann/KernelSU/pull/3800
* ksud: Scan APK comments up to the ZIP length limit by @fhgffy in https://github.com/tiann/KernelSU/pull/3802
* ksud: Skip KMI detection for config-only patches by @fhgffy in https://github.com/tiann/KernelSU/pull/3803
* manager: don't offer kernel install in jailbreak mode by @chisewaguri in https://github.com/tiann/KernelSU/pull/3801
* build(deps): bump the npm group across 1 directory with 19 updates by @dependabot[bot] in https://github.com/tiann/KernelSU/pull/3867
* kernel: throne_tracker: avoid deadlocks on iterate_dir by @backslashxx in https://github.com/tiann/KernelSU/pull/3865
* refactor(kernel): pin manager package name by @5ec1cff in https://github.com/tiann/KernelSU/pull/3868
* fix(kernel): fix sidtab detection by @5ec1cff in https://github.com/tiann/KernelSU/pull/3873
* apk_sign: get_pkg_from_apk_dir_path: move to strrchr by @backslashxx in https://github.com/tiann/KernelSU/pull/3874
* fix(kernel): pkg_observer: defer track_throne to task_work by @yujincheng08 in https://github.com/tiann/KernelSU/pull/3871
* fix(kernel): fix time side-channel detection in selinux hide by @5ec1cff in https://github.com/tiann/KernelSU/pull/3877
* feat: support forcing feature values via ksu_config (incl. boot-patch-v2) by @Ylarod in https://github.com/tiann/KernelSU/pull/3878
* feat(manager): switch module repository to the KernelSU Modules API by @Ylarod in https://github.com/tiann/KernelSU/pull/3880
* feat: block kernel modules at boot via native module_blacklist by @Ylarod in https://github.com/tiann/KernelSU/pull/3881
* feat: add shell_su_adb_only feature to restrict shell root to adb sessions by @Ylarod in https://github.com/tiann/KernelSU/pull/3882
* fix(manager): require explicit opt-in to block kernel modules by @Ylarod in https://github.com/tiann/KernelSU/pull/3883
* build(deps): bump the crates group across 1 directory with 36 updates by @dependabot[bot] in https://github.com/tiann/KernelSU/pull/3870
* manager: Update Indonesian translations by @Neebe3289 in https://github.com/tiann/KernelSU/pull/3748

## New Contributors
* @tpnonthealps made their first contribution in https://github.com/tiann/KernelSU/pull/3685
* @0ctobot made their first contribution in https://github.com/tiann/KernelSU/pull/3738
* @fhgffy made their first contribution in https://github.com/tiann/KernelSU/pull/3802

**Full Changelog**: https://github.com/tiann/KernelSU/compare/v3.3.0...v3.3.1