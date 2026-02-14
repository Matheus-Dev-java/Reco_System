package com.escoladoestudante.reco.service;

import com.escoladoestudante.reco.util.Hashes;
import org.springframework.stereotype.Service;

@Service
public class AbTestingService {
  public String bucketForUser(long userId) {
    var h = Hashes.sha256("ab:" + userId);
    return (Character.digit(h.charAt(0), 16) % 2 == 0) ? "COSINE" : "DOT";
  }

  public String algoForBucket(String bucket) {
    return bucket;
  }
}
