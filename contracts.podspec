Pod::Spec.new do |s|
  s.name         = 'contracts'
  s.version      = '0.1.0'
  s.summary      = 'smart contract and XML'
  s.homepage     = 'https://github.com/alpha-wallet/contracts'
  s.authors      = 'stormbird PTE LTD'
  s.ios.deployment_target = '1.0'
  s.source       = { git: 'https://github.com/alpha-wallet/contracts.git'}
  s.source_files = ""

  s.pod_target_xcconfig = { 'SWIFT_OPTIMIZATION_LEVEL' => '-Owholemodule' }
end
