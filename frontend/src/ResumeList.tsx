import { useEffect, useState } from "react";
import { backendUrl } from "./constants";
import "./Resume.css";

type ResumeResponse = {
  id: number;
  userId: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
};

const ResumeList = () => {
  const [resumes, setResumes] = useState<ResumeResponse[]>([]);
  const [message, setMessage] = useState("Loading resumes...");

  useEffect(() => {
    const loadResumes = async () => {
      /*
        TEMPORARY:
        Replace with session.user.userId once authentication is merged.
      */
      const userId = "TEMP_USER_ID";

      try {
        const response = await fetch(
          `${backendUrl}/api/v1/resumes/users/${encodeURIComponent(userId)}`,
          {
            credentials: "include",
          }
        );

        if (!response.ok) {
          const errorText = await response.text();
          setMessage(errorText || "Could not load resumes.");
          return;
        }

        const data: ResumeResponse[] = await response.json();

        setResumes(data);
        setMessage("");
      } catch (error) {
        console.error("Resume loading error:", error);
        setMessage("Could not connect to the backend.");
      }
    };

    loadResumes();
  }, []);

  return (
    <section className="resume">
      <div className="resume-container">
        <h2 className="resume-title">Your Resumes</h2>

        <p className="resume-subtitle">
          View the resumes you have uploaded to CareerConnect.
        </p>

        {message && <p className="resume-message">{message}</p>}

        {!message && resumes.length === 0 && (
          <p className="resume-message">
            You haven't uploaded any resumes yet.
          </p>
        )}

        {resumes.map((resume) => (
          <div className="resume-selected-file" key={resume.id}>
            <strong>{resume.fileName}</strong>
            <div>
              {(resume.sizeBytes / 1024).toFixed(1)} KB
            </div>
            <div>
              Uploaded: {new Date(resume.uploadedAt).toLocaleDateString()}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};

export default ResumeList;